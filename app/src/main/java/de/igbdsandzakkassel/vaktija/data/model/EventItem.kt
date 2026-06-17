package de.igbdsandzakkassel.vaktija.data.model

/**
 * A community event (lecture, Mevlud, mektep class, Bajram prayer, …), posted by the admin and shown
 * to every user in THEIR app language. Like [NewsItem] the title/body are translated into every app
 * language at post time; additionally [eventAt] is the epoch-millis date/time the event takes place,
 * used to sort upcoming events and to hide past ones.
 */
data class EventItem(
    val id: String,
    val titleByLang: Map<String, String>,
    val bodyByLang: Map<String, String>,
    val sourceLang: String,
    val eventAt: Long,
    val createdAt: Long,
) {
    fun title(lang: String): String = pick(titleByLang, lang)
    fun body(lang: String): String = pick(bodyByLang, lang)

    private fun pick(map: Map<String, String>, lang: String): String =
        map[lang] ?: map[sourceLang] ?: map.values.firstOrNull() ?: ""
}
