package de.igbdsandzakkassel.vaktija.data.remote

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes

/**
 * Remote source of prayer times. Implementation detail (JSON-LD vs HTML scraping vs a future API)
 * is hidden behind this interface so it can be swapped without touching the repository or UI.
 */
interface RemoteVaktijaSource {
    /** Fetches the latest published day's times from vaktija.eu (currently "today"). */
    suspend fun fetchLatest(): DailyTimes
}
