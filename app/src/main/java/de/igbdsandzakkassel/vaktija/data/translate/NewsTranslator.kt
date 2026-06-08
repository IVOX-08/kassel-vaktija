package de.igbdsandzakkassel.vaktija.data.translate

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device (ML Kit) translation of a free-text announcement into every app language. Runs on the
 * admin device at post time; the translations are stored in Firestore so every user just READS the
 * version for their language — no models or network needed on user devices.
 *
 * Bosnian has no dedicated ML Kit model, so Croatian is used (the languages are mutually
 * intelligible) and the result is stored under the "bs" key. Models download on first use
 * (~30 MB each, cached afterwards); a target that fails to translate is omitted and the reader
 * falls back to the source-language text — so an announcement is never lost.
 */
@Singleton
class NewsTranslator @Inject constructor() {

    data class Result(
        val sourceLang: String,
        val titleByLang: Map<String, String>,
        val bodyByLang: Map<String, String>,
        /** App languages whose translation failed (e.g. offline) — readers see the source text. */
        val failedLangs: List<String>,
    )

    /**
     * Detects the source language of [title]+[body] (falling back to [fallbackLang]), keeps the
     * original text under its own key, and translates both fields into every other [AppLanguage].
     */
    suspend fun translateToAll(title: String, body: String, fallbackLang: String): Result {
        val source = detectLanguage("$title\n$body", fallbackLang)
        val titleByLang = linkedMapOf(source to title)
        val bodyByLang = linkedMapOf(source to body)
        val failedLangs = mutableListOf<String>()

        val sourceMlKit = mlKitTag(source)
        for (target in AppLanguage.entries) {
            if (target.tag == source) continue
            val translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceMlKit)
                    .setTargetLanguage(mlKitTag(target.tag))
                    .build(),
            )
            try {
                translator.downloadModelIfNeeded().await()
                if (title.isNotBlank()) titleByLang[target.tag] = translator.translate(title).await()
                if (body.isNotBlank()) bodyByLang[target.tag] = translator.translate(body).await()
            } catch (e: CancellationException) {
                throw e // propagate cancellation; the finally still closes the translator
            } catch (_: Exception) {
                // Couldn't translate this language; record it and fall back to the source text.
                failedLangs += target.tag
            } finally {
                translator.close()
            }
        }
        return Result(source, titleByLang, bodyByLang, failedLangs)
    }

    /** ML Kit language identification, normalised to an app language tag. */
    private suspend fun detectLanguage(text: String, fallbackLang: String): String {
        val client = LanguageIdentification.getClient()
        val identified = runCatching { client.identifyLanguage(text).await() }.getOrNull()
        client.close()
        // Returns a BCP-47 tag or "und". Map Bosnian/Serbian/Croatian → bs and keep only languages
        // the app ships; otherwise use the admin's current UI language.
        val primary = identified?.substringBefore('-')?.lowercase()
        val normalised = when (primary) {
            "hr", "sr", "bs" -> "bs"
            null, "und" -> null
            else -> primary
        }
        return AppLanguage.entries.firstOrNull { it.tag == normalised }?.tag ?: fallbackLang
    }

    private fun mlKitTag(appTag: String): String = when (appTag) {
        "bs" -> TranslateLanguage.CROATIAN // no Bosnian model; Croatian is the same language
        "de" -> TranslateLanguage.GERMAN
        "ar" -> TranslateLanguage.ARABIC
        "tr" -> TranslateLanguage.TURKISH
        "sq" -> TranslateLanguage.ALBANIAN
        "en" -> TranslateLanguage.ENGLISH
        "ur" -> TranslateLanguage.URDU
        "ru" -> TranslateLanguage.RUSSIAN
        else -> TranslateLanguage.ENGLISH
    }
}
