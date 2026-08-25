package de.igbdsandzakkassel.vaktija.data.remote

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Reads prayer times from https://vaktija.eu/<town>.
 *
 * Strategy: the page embeds a schema.org JSON-LD block (Dataset → Schedule → eventSchedule) with
 * the day's times. We parse that rather than scraping styled HTML — it's machine-readable and far
 * more stable. The parser is deliberately strict: if the expected fields are missing it throws,
 * so a page-structure change fails loudly instead of silently returning wrong times.
 */
class VaktijaEuSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : RemoteVaktijaSource {

    override suspend fun fetchLatest(slug: String): DailyTimes = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BASE + slug)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "bs,hr,sr")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "vaktija.eu returned HTTP ${response.code}" }
            val html = response.body?.string() ?: error("vaktija.eu returned an empty body")
            parse(html)
        }
    }

    private fun parse(html: String): DailyTimes {
        // The page can carry SEVERAL ld+json blocks (SEO/breadcrumbs/etc.); scan them ALL for the
        // one holding the Dataset/Schedule instead of assuming it comes first.
        val blocks = LD_JSON_REGEX.findAll(html).map { it.groupValues[1] }.toList()
        if (blocks.isEmpty()) error("vaktija.eu: no JSON-LD block found (page structure changed?)")

        val schedule = blocks.firstNotNullOfOrNull { block ->
            runCatching {
                val root = json.parseToJsonElement(block).jsonObject
                root["@graph"]?.jsonArray
                    ?.map { it.jsonObject }
                    ?.firstOrNull { it["@type"]?.jsonPrimitive?.contentOrNull == "Dataset" }
                    ?.get("mainEntity")?.jsonObject
            }.getOrNull()
        } ?: error("vaktija.eu: no Dataset/Schedule in any JSON-LD block")

        val date = schedule["startDate"]?.jsonPrimitive?.contentOrNull
            ?.let { LocalDate.parse(it) }
            ?: error("vaktija.eu: missing startDate")

        val events = schedule["eventSchedule"]?.jsonArray
            ?: error("vaktija.eu: missing eventSchedule")

        // Map by (normalised) prayer name → time.
        val byName = events.associate { element ->
            val obj = element.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty().lowercase().trim()
            val time = obj["startTime"]?.jsonPrimitive?.contentOrNull
            name to time
        }

        fun time(vararg keys: String): LocalTime {
            for (key in keys) {
                val value = byName.entries.firstOrNull { it.key.contains(key) }?.value
                if (value != null) return LocalTime.parse(value)
            }
            error("vaktija.eu: missing time for ${keys.joinToString("/")}")
        }

        val result = DailyTimes(
            date = date,
            fajr = time("sabah", "imsak", "zora", "fajr"),
            sunrise = time("izlazak", "sunrise"),
            dhuhr = time("podne", "dhuhr", "zuhr"),
            asr = time("ikindija", "asr"),
            maghrib = time("akšam", "aksam", "maghrib"),
            isha = time("jacija", "isha", "jacaja"),
        )
        // Plausibility gate: fail loudly rather than cache garbage if the site ever serves mixed-up
        // values. (Isha is deliberately NOT checked against Maghrib — at Kassel's latitude it can
        // cross midnight in summer, e.g. 01:21, which is smaller as a plain time-of-day.)
        check(
            result.fajr < result.sunrise &&
                result.sunrise < result.dhuhr &&
                result.dhuhr < result.asr &&
                result.asr < result.maghrib,
        ) { "vaktija.eu: implausible times (order fajr<sunrise<dhuhr<asr<maghrib violated)" }
        // Staleness gate: the known edge-cache lag is ±1 day. Anything further off means the page is
        // frozen/broken — better to fail (and show the in-app stale banner) than to re-stamp
        // week-old times as "today's" forever.
        check(kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now())) <= 2) {
            "vaktija.eu: startDate $date too far from today (frozen page?)"
        }
        return result
    }

    private companion object {
        const val BASE = "https://vaktija.eu/"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0 Mobile Safari/537.36 KasselVaktija"
        val LD_JSON_REGEX =
            Regex("""<script[^>]*application/ld\+json[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
    }
}
