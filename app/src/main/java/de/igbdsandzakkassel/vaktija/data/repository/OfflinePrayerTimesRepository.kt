package de.igbdsandzakkassel.vaktija.data.repository

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
) : PrayerTimesRepository {

    // Emits the current date, re-checking once a minute so the day rolls over at midnight.
    private val currentDate: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    override fun observeToday(): Flow<DailyTimes?> =
        currentDate.flatMapLatest { date ->
            combine(dao.observe(date.toString()), dao.observeLatest(date.toString())) { today, latest ->
                (today ?: latest)?.toModel()
            }
        }

    override fun observeFreshness(): Flow<Boolean> =
        currentDate.flatMapLatest { date ->
            dao.observe(date.toString()).map { it != null }
        }

    override suspend fun refresh(): Boolean = try {
        val times = remote.fetchLatest()
        val today = LocalDate.now()
        // vaktija.eu/kassel always publishes the CURRENT day's times, but its JSON-LD `startDate`
        // can lag by a day (e.g. just after midnight / edge caching). Trusting it would key today's
        // correct times under yesterday's date and wrongly show the "offline/stale" banner. So key
        // the cache by the device's current date instead.
        dao.upsert(times.copy(date = today).toEntity(fetchedAt = System.currentTimeMillis()))
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
