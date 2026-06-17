package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import de.igbdsandzakkassel.vaktija.data.model.EventItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community events backed by the Firestore `events` collection. Everyone reads (cached offline); only
 * the admin writes (the existing `match /{document=**}` rule covers this collection too). Sorted by
 * the event date/time in memory (soonest first) — no composite index required. Mirrors the per-
 * language map layout of the news collection.
 */
@Singleton
class FirestoreEventsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EventsRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observeEvents(): Flow<List<EventItem>?> = callbackFlow {
        val registration = collection.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.documents
                ?.mapNotNull { it.toEventItem() }
                ?.sortedBy { it.eventAt }
                ?: emptyList()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun postEvent(
        titleByLang: Map<String, String>,
        bodyByLang: Map<String, String>,
        sourceLang: String,
        eventAt: Long,
    ) {
        // Fire-and-forget (commits to the local cache immediately, syncs when online).
        collection.document().set(
            mapOf(
                "title" to titleByLang,
                "body" to bodyByLang,
                "sourceLang" to sourceLang,
                "eventAt" to eventAt,
                "createdAt" to System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteEvent(id: String) {
        collection.document(id).delete()
    }

    private fun DocumentSnapshot.toEventItem(): EventItem? {
        val sourceLang = getString("sourceLang") ?: AppLanguage.DEFAULT.tag
        val title = readLangMap("title", sourceLang) ?: return null
        return EventItem(
            id = id,
            titleByLang = title,
            bodyByLang = readLangMap("body", sourceLang) ?: emptyMap(),
            sourceLang = sourceLang,
            eventAt = getLong("eventAt") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

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
        const val COLLECTION = "events"
    }
}
