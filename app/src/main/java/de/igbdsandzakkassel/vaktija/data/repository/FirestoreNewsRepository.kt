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
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreNewsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val communityRepository: CommunityRepository,
) : NewsRepository {

    /** Announcements of one community. */
    private fun newsOf(communityId: String) =
        firestore.collection(COMMUNITIES).document(communityId).collection(NEWS)

    private fun imagesOf(communityId: String) =
        firestore.collection(COMMUNITIES).document(communityId).collection(IMAGES)

    /** The head admin's announcements to every community. */
    private val broadcasts get() = firestore.collection(BROADCASTS)
    private val broadcastImages get() = firestore.collection(BROADCAST_IMAGES)

    /**
     * The selected community's announcements merged with the head admin's federation-wide ones,
     * newest first. Merged rather than shown apart: to the reader they are all "news from the
     * mosque", and a separate tab for the rare federation notice would mostly sit empty.
     */
    override fun observeNews(): Flow<List<NewsItem>?> =
        communityRepository.observeSelection().flatMapLatest { selection ->
            val communityId = selection?.community?.id
            combine(
                if (communityId == null) flowOf(emptyList()) else observeCollection(newsOf(communityId), false),
                observeCollection(broadcasts, true),
            ) { own, all ->
                // Filtered here rather than in the query: "addressed to everyone" is stored as an
                // empty list, which no array_contains query can express, and the number of
                // federation announcements is small enough that the client can decide.
                (own + all.filter { it.reaches(communityId) })
                    .sortedByDescending { it.createdAt }
            }
        }

    private fun observeCollection(
        ref: com.google.firebase.firestore.Query,
        broadcast: Boolean,
    ): Flow<List<NewsItem>> = callbackFlow {
        val registration = ref.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents?.mapNotNull { it.toNewsItem(broadcast) } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getLatestNews(): List<NewsItem>? = runCatching {
        val communityId = communityRepository.observeSelection().first()?.community?.id
        val own = if (communityId == null) emptyList() else
            newsOf(communityId).get().await().documents.mapNotNull { it.toNewsItem(false) }
        val all = broadcasts.get().await().documents
            .mapNotNull { it.toNewsItem(true) }
            .filter { it.reaches(communityId) }
        (own + all).sortedByDescending { it.createdAt }
    }.getOrNull()

    override suspend fun postNews(
        titleByLang: Map<String, String>,
        bodyByLang: Map<String, String>,
        sourceLang: String,
        imageJpeg: ByteArray?,
        broadcast: Boolean,
        audience: List<String>,
    ) {
        val communityId = communityRepository.observeSelection().first()?.community?.id
        if (!broadcast && communityId == null) return
        val target = if (broadcast) broadcasts else newsOf(communityId!!)
        val imageTarget = if (broadcast) broadcastImages else imagesOf(communityId!!)
        // Pre-allocate the id so the image can be written to the matching image doc.
        val doc = target.document()
        // Fire-and-forget: Firestore commits to the local cache immediately and syncs when online
        // (awaiting the Task would block indefinitely while offline).
        if (imageJpeg != null) {
            // Write the image FIRST (best-effort) so it is on the device before the announcement
            // appears; readers tolerate a missing image regardless of ordering. Fire-and-forget, but
            // log a server rejection (the most likely cause is the news_images security rule not yet
            // being published — see docs/firestore/RULES.md) so a silent no-image is at least traceable.
            imageTarget.document(doc.id).set(
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
                "audience" to if (broadcast) audience else emptyList(),
                "communityId" to if (broadcast) null else communityId,
                "likeCount" to 0,
                "dislikeCount" to 0,
            ),
        )
    }

    override suspend fun deleteNews(item: NewsItem) {
        val communityId = communityRepository.observeSelection().first()?.community?.id
        if (!item.isBroadcast && communityId == null) return
        val target = if (item.isBroadcast) broadcasts else newsOf(communityId!!)
        val imageTarget = if (item.isBroadcast) broadcastImages else imagesOf(communityId!!)
        target.document(item.id).delete()
        // Remove the attached image too (no-op if there wasn't one).
        imageTarget.document(item.id).delete()
    }

    override suspend fun getNewsImage(item: NewsItem): ByteArray? = runCatching {
        val communityId = communityRepository.observeSelection().first()?.community?.id
        if (!item.isBroadcast && communityId == null) return null
        val imagesCollection =
            if (item.isBroadcast) broadcastImages else imagesOf(communityId!!)
        val id = item.id
        // Cache-first: a picture already seen on this device is read from local cache and never
        // re-downloaded; only the first view of a given image hits the network.
        val snapshot = runCatching { imagesCollection.document(id).get(Source.CACHE).await() }
            .getOrNull()
            ?.takeIf { it.exists() }
            ?: imagesCollection.document(id).get(Source.SERVER).await()
        val data = snapshot.getString("data")?.takeIf { it.isNotBlank() } ?: return null
        Base64.decode(data, Base64.NO_WRAP)
    }.getOrNull()

    private fun DocumentSnapshot.toNewsItem(broadcast: Boolean): NewsItem? {
        val sourceLang = getString("sourceLang") ?: AppLanguage.DEFAULT.tag
        val title = readLangMap("title", sourceLang) ?: return null
        return NewsItem(
            id = id,
            titleByLang = title,
            bodyByLang = readLangMap("body", sourceLang) ?: emptyMap(),
            sourceLang = sourceLang,
            createdAt = getLong("createdAt") ?: 0L,
            hasImage = getBoolean("hasImage") ?: false,
            isBroadcast = broadcast,
            audience = (get("audience") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            communityId = getString("communityId"),
            likeCount = (getLong("likeCount") ?: 0L).toInt(),
            dislikeCount = (getLong("dislikeCount") ?: 0L).toInt(),
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
        const val COMMUNITIES = "communities"
        const val NEWS = "news"
        const val IMAGES = "news_images"
        const val BROADCASTS = "broadcasts"
        const val BROADCAST_IMAGES = "broadcast_images"
        const val TAG = "FirestoreNews"
    }
}
