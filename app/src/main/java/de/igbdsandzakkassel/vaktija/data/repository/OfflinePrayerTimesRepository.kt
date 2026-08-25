package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.local.PrayerTimesDao
import de.igbdsandzakkassel.vaktija.data.local.toEntity
import de.igbdsandzakkassel.vaktija.data.local.toModel
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.remote.RemoteVaktijaSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first prayer-times repository: the UI always reads from the Room cache; the network is
 * only used to keep that cache up to date (via [refresh], called on app open and by WorkManager).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class OfflinePrayerTimesRepository @Inject constructor(
    private val dao: PrayerTimesDao,
    private val remote: RemoteVaktijaSource,
    private val communityRepository: CommunityRepository,
) : PrayerTimesRepository {

    // The town whose times this device shows. Everything below is scoped to it, so switching town
    // (or community) re-points the cache reads and the next fetch without any extra plumbing.
    private val location = communityRepository.observeLocation().distinctUntilChanged()

    // Emits the current date, re-checking once a minute so the day rolls over at midnight.
    private val currentDate: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    override fun observeToday(): Flow<DailyTimes?> =
        combine(location, currentDate) { loc, date -> loc to date }
            .flatMapLatest { (loc, date) ->
                if (loc == null) return@flatMapLatest flow { emit(null) }
                combine(
                    dao.observe(loc.id, date.toString()),
                    dao.observeLatest(loc.id, date.toString()),
                ) { today, latest -> (today ?: latest)?.toModel() }
            }

    override fun observeFreshness(): Flow<Boolean> =
        combine(location, currentDate) { loc, date -> loc to date }
            .flatMapLatest { (loc, date) ->
                if (loc == null) return@flatMapLatest flow { emit(false) }
                dao.observe(loc.id, date.toString()).map { it != null }
            }

    override suspend fun refresh(): Boolean = try {
        // Whichever town is selected right now; null only before the catalogue has loaded at all.
        val loc = location.first() ?: error("no location selected yet")
        val times = remote.fetchLatest(loc.vaktijaSlug)
        val today = LocalDate.now()
        // vaktija.eu always publishes the CURRENT day's times for the requested town, but its JSON-LD `startDate`
        // can lag by a day (e.g. just after midnight / edge caching). Trusting it would key today's
        // correct times under yesterday's date and wrongly show the "offline/stale" banner. So key
        // the cache by the device's current date instead.
        dao.upsert(times.copy(date = today).toEntity(loc.id, System.currentTimeMillis()))
        // Keep the cache small: drop anything older than ~2 weeks, and clean up any future-dated
        // row a transiently wrong device clock may have left behind (it would otherwise shadow
        // real data in the observeLatest fallback forever).
        dao.deleteOlderThan(today.minusWeeks(2).toString())
        dao.deleteNewerThan(today.plusDays(1).toString())
        true
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e // never swallow a cancelled worker's cancellation
    } catch (e: Exception) {
        false
    }
}
