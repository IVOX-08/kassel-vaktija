package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.EventItem
import kotlinx.coroutines.flow.Flow

/**
 * Source of community events. Backed by a Firestore collection (public read, admin-only write,
 * enforced by the same server rule as the news collection). Each event carries per-language
 * translations + the date/time it takes place.
 */
interface EventsRepository {
    /** Soonest-first stream of events; null while the first snapshot is still loading. */
    fun observeEvents(): Flow<List<EventItem>?>

    /** Admin-only: publish a new event (already translated into every app language). */
    suspend fun postEvent(
        titleByLang: Map<String, String>,
        bodyByLang: Map<String, String>,
        sourceLang: String,
        eventAt: Long,
    )

    /** Admin-only: remove an event by its document id. */
    suspend fun deleteEvent(id: String)
}
