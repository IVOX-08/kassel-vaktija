package de.igbdsandzakkassel.vaktija.data.remote

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes

/**
 * Remote source of prayer times. Implementation detail (JSON-LD vs HTML scraping vs a future API)
 * is hidden behind this interface so it can be swapped without touching the repository or UI.
 */
interface RemoteVaktijaSource {
    /**
     * Fetches the latest published day's times for one town (currently "today").
     *
     * [slug] is the town's path on vaktija.eu — "kassel", "hann-munden", … The site publishes ~1300
     * German towns in an identical format, so supporting every IGBD community is a matter of
     * passing the right slug rather than of a second data source.
     */
    suspend fun fetchLatest(slug: String): DailyTimes
}
