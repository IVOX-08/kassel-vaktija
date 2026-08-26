package de.igbdsandzakkassel.vaktija.data.model

/**
 * A single community announcement, posted by the admin and shown to every user in the News tab in
 * THEIR app language. The admin writes it once (in Bosnian/German/Arabic/…); it is translated into
 * every app language at post time and stored as a per-language map in Firestore (cached offline).
 *
 * [createdAt] is a client-set epoch-millis timestamp so the list orders correctly even before a
 * server round-trip. [sourceLang] is the language the admin actually wrote in (used as the display
 * fallback when a particular translation is missing).
 *
 * [hasImage] is true when the admin attached a flyer/picture. The image bytes are NOT carried here —
 * they live in a separate `news_images/{id}` document and are fetched lazily (only when a card is
 * actually shown), so the list stays light. This flag just tells the UI to render an image slot.
 */
data class NewsItem(
    val id: String,
    val titleByLang: Map<String, String>,
    val bodyByLang: Map<String, String>,
    val sourceLang: String,
    val createdAt: Long,
    val hasImage: Boolean = false,
    /**
     * True for an announcement from the head admin, which reaches every community rather than one.
     * Kept on the item because it decides where the document lives — and therefore where a delete
     * has to go.
     */
    val isBroadcast: Boolean = false,
    /**
     * Which communities a broadcast is addressed to. **Empty means everyone** — that is the common
     * case and keeps older announcements, written before this existed, reaching everyone rather
     * than silently reaching nobody.
     *
     * Only meaningful for a broadcast; a community's own announcement is already addressed by where
     * it is stored.
     */
    val audience: List<String> = emptyList(),
) {
    /** Whether this item should be shown to a member of [communityId]. */
    fun reaches(communityId: String?): Boolean =
        !isBroadcast || audience.isEmpty() || (communityId != null && communityId in audience)

    /** Title in [lang], falling back to the source language, then any available text. */
    fun title(lang: String): String = pick(titleByLang, lang)

    /** Body in [lang], falling back to the source language, then any available text. */
    fun body(lang: String): String = pick(bodyByLang, lang)

    private fun pick(map: Map<String, String>, lang: String): String =
        map[lang] ?: map[sourceLang] ?: map.values.firstOrNull() ?: ""
}
