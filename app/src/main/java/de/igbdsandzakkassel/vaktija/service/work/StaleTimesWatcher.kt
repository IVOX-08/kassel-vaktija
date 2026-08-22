package de.igbdsandzakkassel.vaktija.service.work

import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the cached prayer times current in a process that never restarts.
 *
 * The mosque's TV board runs the app 24/7. Every other refresh trigger we have is tied to a
 * (re)start — the Application's one-shot worker fires on process create, and the dashboard's
 * ViewModel refreshes in `init`, which on the TV happens exactly once, ever. At midnight the app
 * correctly NOTICES the new day (the date flow re-emits, the stale banner appears) but nothing
 * ever went out to fetch the new day's times, so the board sat on yesterday's numbers until
 * someone force-stopped the app by hand. WorkManager's daily periodic job is not a dependable
 * answer on its own either: on a TV it competes with Doze/app-standby, and its run time inside the
 * 24 h period is not ours to choose.
 *
 * So: watch freshness directly. Whenever the cache holds no row for today, keep fetching until it
 * succeeds — which also covers the realistic failure mode of the mosque's Wi-Fi not being up at
 * 00:05. The gaps start short and widen ([RETRY_STEPS_MS]) so a brief outage is recovered within a
 * minute rather than leaving yesterday's times on a public wall for the whole morning, while a long
 * outage settles into a quiet poll. When the data is fresh this collector is idle and costs nothing.
 */
@Singleton
class StaleTimesWatcher @Inject constructor(
    private val repository: PrayerTimesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            // collectLatest: when freshness flips to true the pending retry loop is cancelled.
            repository.observeFreshness().collectLatest { fresh ->
                if (fresh) return@collectLatest
                var attempt = 0
                while (true) {
                    if (repository.refresh()) return@collectLatest
                    delay(RETRY_STEPS_MS[attempt.coerceAtMost(RETRY_STEPS_MS.lastIndex)])
                    attempt++
                }
            }
        }
    }

    private companion object {
        // 30 s, 1 min, 2 min, 5 min, then every 10 min for as long as the outage lasts.
        val RETRY_STEPS_MS = longArrayOf(30_000, 60_000, 120_000, 300_000, 600_000)
    }
}
