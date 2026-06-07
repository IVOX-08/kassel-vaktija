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
            combine(dao.observe(date.toString()), dao.observeLatest()) { today, latest ->
                (today ?: latest)?.toModel()
            }
        }

    override fun observeFreshness(): Flow<Boolean> =
        currentDate.flatMapLatest { date ->
            dao.observe(date.toString()).map { it != null }
        }

    override suspend fun refresh(): Boolean = try {
        val times = remote.fetchLatest()
        dao.upsert(times.toEntity(fetchedAt = System.currentTimeMillis()))
        // Keep the cache small: drop anything older than ~2 weeks.
        dao.deleteOlderThan(LocalDate.now().minusWeeks(2).toString())
        true
    } catch (e: Exception) {
        false
    }
}
