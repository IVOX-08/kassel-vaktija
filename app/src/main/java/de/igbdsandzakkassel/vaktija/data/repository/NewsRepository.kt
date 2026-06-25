package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import kotlinx.coroutines.flow.Flow

/**
 * Source of community announcements. Backed by a Firestore collection so the admin can post/delete
 * and every device picks up the change (cached offline). Everyone reads; only the admin can write
 * (enforced by the Firestore security rule on the server). Each announcement carries per-language
 * translations so every user reads it in their own app language.
 */
interface NewsRepository {
    /** Newest-first stream of announcements; null while the first snapshot is still loading. */
    fun observeNews(): Flow<List<NewsItem>?>

    /**
     * One-shot newest-first fetch (no listener) for background checks (e.g. deciding whether to
     * post a "new announcement" notification). Returns null on failure.
     */
    suspend fun getLatestNews(): List<NewsItem>?

    /**
     * Admin-only: publish a new announcement (already translated into every app language).
     * [imageJpeg], when non-null, is an already-compressed JPEG that is stored alongside the
     * announcement (in a separate `news_images` document) and shown to every reader.
     */
    suspend fun postNews(
        titleByLang: Map<String, String>,
        bodyByLang: Map<String, String>,
        sourceLang: String,
        imageJpeg: ByteArray? = null,
    )

    /** Admin-only: remove an announcement (and its attached image, if any) by its document id. */
    suspend fun deleteNews(id: String)

    /**
     * Fetches the attached flyer/image bytes for [id], or null if the announcement has none or the
     * fetch fails. Cache-first (an image already seen on this device never re-downloads), so it is
     * cheap to call lazily when a card scrolls into view.
     */
    suspend fun getNewsImage(id: String): ByteArray?
}
