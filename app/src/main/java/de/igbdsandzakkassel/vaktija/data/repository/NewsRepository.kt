package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import kotlinx.coroutines.flow.Flow

/**
 * Source of community announcements. Backed by a Firestore collection so the admin can post/delete
 * and every device picks up the change (cached offline). Everyone reads; only the admin can write
 * (enforced by the Firestore security rule on the server).
 */
interface NewsRepository {
    /** Newest-first stream of announcements; null while the first snapshot is still loading. */
    fun observeNews(): Flow<List<NewsItem>?>

    /** Admin-only: publish a new announcement. */
    suspend fun postNews(title: String, body: String)

    /** Admin-only: remove an announcement by its document id. */
    suspend fun deleteNews(id: String)
}
