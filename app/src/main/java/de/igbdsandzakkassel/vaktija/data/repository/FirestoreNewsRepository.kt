package de.igbdsandzakkassel.vaktija.data.repository

import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
 *
 * An attached flyer/image is stored as a Base64 JPEG in a SEPARATE `news_images/{id}` document
 * (same id as the announcement). Keeping it out of the announcement doc keeps the feed listener
 * light: the list query never downloads image bytes, only the `hasImage` flag, and each picture is
 * fetched lazily and cache-first when its card is shown. This needs no Cloud Storage (works on the
 * free Firestore plan); the per-image doc just has to stay under Firestore's 1 MB document limit,
 * which the admin-side compressor enforces.
 */
@Singleton
class FirestoreNewsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : NewsRepository {

    private val collection get() = firestore.collection(COLLECTION)
    private val imagesCollection get() = firestore.collection(IMAGES_COLLECTION)

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
        imageJpeg: ByteArray?,
    ) {
        // Pre-allocate the id so the image can be written to the matching news_images doc.
        val doc = collection.document()
        // Fire-and-forget: Firestore commits to the local cache immediately and syncs when online
        // (awaiting the Task would block indefinitely while offline).
        if (imageJpeg != null) {
            // Write the image FIRST (best-effort) so it is on the device before the announcement
            // appears; readers tolerate a missing image regardless of ordering. Fire-and-forget, but
            // log a server rejection (the most likely cause is the news_images security rule not yet
            // being published — see docs/firestore/RULES.md) so a silent no-image is at least traceable.
            imagesCollection.document(doc.id).set(
                mapOf("data" to Base64.encodeToString(imageJpeg, Base64.NO_WRAP)),
            ).addOnFailureListener { e ->
                Log.w(TAG, "Announcement image upload failed for ${doc.id}; the flyer won't appear", e)
            }
        }
        doc.set(
            mapOf(
                "title" to titleByLang,
                "body" to bodyByLang,
                "sourceLang" to sourceLang,
                "createdAt" to System.currentTimeMillis(),
                "hasImage" to (imageJpeg != null),
            ),
        )
    }

    override suspend fun deleteNews(id: String) {
        collection.document(id).delete()
        // Remove the attached image too (no-op if there wasn't one).
        imagesCollection.document(id).delete()
    }

    override suspend fun getNewsImage(id: String): ByteArray? = runCatching {
        // Cache-first: a picture already seen on this device is read from local cache and never
        // re-downloaded; only the first view of a given image hits the network.
        val snapshot = runCatching { imagesCollection.document(id).get(Source.CACHE).await() }
            .getOrNull()
            ?.takeIf { it.exists() }
            ?: imagesCollection.document(id).get(Source.SERVER).await()
        val data = snapshot.getString("data")?.takeIf { it.isNotBlank() } ?: return null
        Base64.decode(data, Base64.NO_WRAP)
    }.getOrNull()

    private fun DocumentSnapshot.toNewsItem(): NewsItem? {
        val sourceLang = getString("sourceLang") ?: AppLanguage.DEFAULT.tag
        val title = readLangMap("title", sourceLang) ?: return null
        return NewsItem(
            id = id,
            titleByLang = title,
            bodyByLang = readLangMap("body", sourceLang) ?: emptyMap(),
            sourceLang = sourceLang,
            createdAt = getLong("createdAt") ?: 0L,
            hasImage = getBoolean("hasImage") ?: false,
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
        const val IMAGES_COLLECTION = "news_images"
        const val TAG = "FirestoreNews"
    }
}
