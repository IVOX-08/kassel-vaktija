package de.igbdsandzakkassel.vaktija.data.translate

import de.igbdsandzakkassel.vaktija.BuildConfig
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-quality announcement translation via Google's Gemini (LLM). Runs only on the admin device at
 * post time; the 8-language result is stored in Firestore so users just read their language.
 *
 * An LLM gives far more natural results than the on-device [NewsTranslator] (ML Kit) — especially for
 * Arabic and for Islamic terminology (Bajram→Eid, Džuma→Jumuʿah, …) — handled directly in the prompt.
 * Returns null when not configured (no API key) or on any failure, so the caller can fall back to
 * ML Kit and a post never breaks.
 */
@Singleton
class GeminiTranslator @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    fun isConfigured(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun translateToAll(title: String, body: String, fallbackLang: String): NewsTranslator.Result? {
        if (!isConfigured() || (title.isBlank() && body.isBlank())) return null
        return withContext(Dispatchers.IO) {
            runCatching { request(title, body, fallbackLang) }.getOrNull()
        }
    }

    private fun request(title: String, body: String, fallbackLang: String): NewsTranslator.Result {
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", buildPrompt(title, body)) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
        }.toString()

        val httpRequest = Request.Builder()
            .url("$ENDPOINT?key=${BuildConfig.GEMINI_API_KEY}")
            .post(requestBody.toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Gemini HTTP ${response.code}" }

            // The model's JSON answer is in candidates[0].content.parts. Pick the first part that
            // actually has non-blank text — newer "thinking" models can prepend a thought part.
            val answer = json.parseToJsonElement(payload).jsonObject["candidates"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.firstNotNullOfOrNull {
                    it.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.takeIf { t -> t.isNotBlank() }
                }
                ?: error("Gemini: empty response")

            val parsed = json.parseToJsonElement(stripFences(answer)).jsonObject
            val validTags = AppLanguage.entries.map { it.tag }.toSet()
            val source = parsed["sourceLang"]?.jsonPrimitive?.contentOrNull
                ?.lowercase()?.substringBefore('-')
                ?.let { tag -> if (tag in validTags) tag else null }
                ?: fallbackLang

            val titleByLang = readLangMap(parsed["title"], validTags).toMutableMap()
            val bodyByLang = readLangMap(parsed["body"], validTags).toMutableMap()

            // Guarantee the admin's exact original text under its own language.
            titleByLang[source] = title
            if (body.isNotBlank()) bodyByLang[source] = body

            // Any app language that the model didn't return a (title) translation for.
            val failed = validTags.filter { it != source && titleByLang[it].isNullOrBlank() && title.isNotBlank() }

            return NewsTranslator.Result(source, titleByLang, bodyByLang, failed)
        }
    }

    private fun readLangMap(element: kotlinx.serialization.json.JsonElement?, validTags: Set<String>): Map<String, String> {
        val obj = element?.jsonObject ?: return emptyMap()
        return buildMap {
            for ((key, value) in obj) {
                val tag = key.lowercase()
                val text = value.jsonPrimitive.contentOrNull
                if (tag in validTags && !text.isNullOrBlank()) put(tag, text)
            }
        }
    }

    private fun stripFences(text: String): String =
        text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private fun buildPrompt(title: String, body: String): String = """
        You are a professional translator for the mobile app of the Bosniak (Sandžak) Muslim community
        mosque "IGBD-Gemeinde Sandžak-Kassel" in Kassel, Germany. You translate short community
        announcements (prayer/event notices) for ordinary members.

        Translate the announcement below into ALL of these languages and return STRICT JSON.
        Use these exact keys: bs=Bosnian, de=German, ar=Modern Standard Arabic, tr=Turkish,
        sq=Albanian, en=English, ur=Urdu, ru=Russian.

        Rules:
        - Faithful, natural, fluent translation. Preserve meaning, tone, dates, times, numbers, names
          and line breaks. Do NOT add or omit information.
        - Use the CORRECT Islamic/religious term in each language; never transliterate religious terms
          literally. Examples: Bajram/Bayram = Eid (ar: العيد; the Eid prayer = صلاة العيد);
          namaz / Gebet = the ritual prayer (ar: الصلاة); Džuma = Jumuʿah (ar: صلاة الجمعة);
          Teravija = Tarawih (ar: صلاة التراويح); iftar (ar: الإفطار); sehur (ar: السحور);
          Ramazan = Ramadan (ar: رمضان); Mevlud = Mawlid; sadaka = sadaqah; abdest = wudu.
        - Arabic must be correct, natural Modern Standard Arabic — not a word-for-word gloss.
        - Detect the source language (one of the keys above) and report it as "sourceLang".

        Return ONLY a JSON object, no markdown, with exactly this shape:
        {"sourceLang":"<key>","title":{"bs":"","de":"","ar":"","tr":"","sq":"","en":"","ur":"","ru":""},"body":{"bs":"","de":"","ar":"","tr":"","sq":"","en":"","ur":"","ru":""}}

        TITLE:
        $title

        BODY:
        $body
    """.trimIndent()

    private companion object {
        // Gemini 3.5 Flash (GA): a generation newer than 2.5-flash → noticeably better, especially
        // for Arabic + Islamic terminology, and still on the free tier (no billing). Pro models went
        // paid-only in April 2026, so Flash is the best free choice. Verified working on the free key.
        const val MODEL = "gemini-3.5-flash"
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
