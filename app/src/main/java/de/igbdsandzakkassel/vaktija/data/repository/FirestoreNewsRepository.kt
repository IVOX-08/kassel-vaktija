package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community announcements backed by the Firestore `news` collection. Everyone reads it (Firestore
 * caches it offline); only the admin can write (enforced by the server security rule). The list is
 * sorted newest-first in memory, so no composite index or per-document `orderBy` is required.
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

    override suspend fun postNews(title: String, body: String) {
        // Fire-and-forget: Firestore commits to the local cache immediately and syncs when online
        // (awaiting the Task would block indefinitely while offline).
        collection.document().set(
            mapOf(
                "title" to title,
                "body" to body,
                "createdAt" to System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteNews(id: String) {
        collection.document(id).delete()
    }

    private fun DocumentSnapshot.toNewsItem(): NewsItem? {
        val title = getString("title") ?: return null
        return NewsItem(
            id = id,
            title = title,
            body = getString("body").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    private companion object {
        const val COLLECTION = "news"
    }
}
