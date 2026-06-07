package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Placeholder times — kept for previews/tests only. The real source (Phase 1) is
 * [OfflinePrayerTimesRepository], which is what [de.igbdsandzakkassel.vaktija.di.DataModule] binds.
 */
class StubPrayerTimesRepository @Inject constructor() : PrayerTimesRepository {
    override fun observeToday(): Flow<DailyTimes?> = flow {
        emit(
            DailyTimes(
                date = LocalDate.now(),
                fajr = LocalTime.of(3, 33),
                sunrise = LocalTime.of(5, 5),
                dhuhr = LocalTime.of(13, 25),
                asr = LocalTime.of(17, 43),
                maghrib = LocalTime.of(21, 35),
                isha = LocalTime.of(23, 5),
            ),
        )
    }

    override fun observeFreshness(): Flow<Boolean> = flowOf(true)

    override suspend fun refresh(): Boolean = true
}
