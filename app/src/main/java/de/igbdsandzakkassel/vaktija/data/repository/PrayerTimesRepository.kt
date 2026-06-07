package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import kotlinx.coroutines.flow.Flow

/**
 * Source of daily prayer times. Backed by vaktija.eu + a Room cache (offline-first). The dashboard
 * depends only on this interface.
 */
interface PrayerTimesRepository {
    /** Today's times — falls back to the most recent cached day when today isn't available yet. */
    fun observeToday(): Flow<DailyTimes?>

    /** True when the shown times are actually for the current day (false = showing stale cache). */
    fun observeFreshness(): Flow<Boolean>

    /** Fetches the latest times from the network and caches them. Returns true on success. */
    suspend fun refresh(): Boolean
}
