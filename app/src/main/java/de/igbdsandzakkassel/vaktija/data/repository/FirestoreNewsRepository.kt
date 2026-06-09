package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community announcements backed by the Firestore `news` collection. Everyone reads it (Firestore
 * caches it offline); only the admin can write (enforced by the server security rule). The list is
 * sorted newest-first in memory, so no composite index or per-document `orderBy` is required.
 *
 * Each document stores `title` and `body` as per-language maps ({"bs":"…","de":"…",…}) plus the
 * `sourceLang` the admin wrote in. Legacy documents that stored a plain string are still read
 * (treated as a single source-language entry).
 */
@Singleton
class FirestoreNewsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : NewsRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observeNews(): Flow<List<NewsItem>?> = callbackFlow {
        val registration = collection.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.documents
                ?.mapNotNull { it.toNewsItem() }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getLatestNews(): List<NewsItem>? =
        runCatching {
            collection.get().await().documents
                .mapNotNull { it.toNewsItem() }
                .sortedByDescending { it.createdAt }
        }.getOrNull()

    override suspend fun postNews(
        titleByLang: Map<String, String>,
        bodyByLang: Map<String, String>,
        sourceLang: String,
    ) {
        // Fire-and-forget: Firestore commits to the local cache immediately and syncs when online
        // (awaiting the Task would block indefinitely while offline).
        collection.document().set(
            mapOf(
                "title" to titleByLang,
                "body" to bodyByLang,
                "sourceLang" to sourceLang,
                "createdAt" to System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteNews(id: String) {
        collection.document(id).delete()
    }

    private fun DocumentSnapshot.toNewsItem(): NewsItem? {
        val sourceLang = getString("sourceLang") ?: AppLanguage.DEFAULT.tag
        val title = readLangMap("title", sourceLang) ?: return null
        return NewsItem(
            id = id,
            titleByLang = title,
            bodyByLang = readLangMap("body", sourceLang) ?: emptyMap(),
            sourceLang = sourceLang,
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    /** Reads a per-language map field, also accepting a legacy plain-string value. */
    private fun DocumentSnapshot.readLangMap(field: String, sourceLang: String): Map<String, String>? =
        when (val raw = get(field)) {
            is Map<*, *> -> raw.entries
                .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }
                .toMap()
                .ifEmpty { null }
            is String -> raw.takeIf { it.isNotBlank() }?.let { mapOf(sourceLang to it) }
            else -> null
        }

    private companion object {
        const val COLLECTION = "news"
    }
}
