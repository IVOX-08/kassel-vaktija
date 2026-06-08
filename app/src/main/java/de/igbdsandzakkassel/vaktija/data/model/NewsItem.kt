package de.igbdsandzakkassel.vaktija.data.model

/**
 * A single community announcement, posted by the admin and shown to every user in the News tab in
 * THEIR app language. The admin writes it once (in Bosnian/German/Arabic/…); it is translated into
 * every app language at post time and stored as a per-language map in Firestore (cached offline).
 *
 * [createdAt] is a client-set epoch-millis timestamp so the list orders correctly even before a
 * server round-trip. [sourceLang] is the language the admin actually wrote in (used as the display
 * fallback when a particular translation is missing).
 */
data class NewsItem(
    val id: String,
    val titleByLang: Map<String, String>,
    val bodyByLang: Map<String, String>,
    val sourceLang: String,
    val createdAt: Long,
) {
    /** Title in [lang], falling back to the source language, then any available text. */
    fun title(lang: String): String = pick(titleByLang, lang)

    /** Body in [lang], falling back to the source language, then any available text. */
    fun body(lang: String): String = pick(bodyByLang, lang)

    private fun pick(map: Map<String, String>, lang: String): String =
        map[lang] ?: map[sourceLang] ?: map.values.firstOrNull() ?: ""
}
