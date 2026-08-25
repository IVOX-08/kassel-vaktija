package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {

    @Upsert
    suspend fun upsert(entity: DailyTimesEntity)

    @Query("SELECT * FROM daily_times WHERE locationId = :locationId AND date = :date LIMIT 1")
    fun observe(locationId: String, date: String): Flow<DailyTimesEntity?>

    /**
     * Most recently dated row up to [maxDate] — used as an offline fallback when today isn't cached
     * yet. Bounded so a future-dated "poison" row (written while the device clock was wrong) can
     * never shadow the real data.
     */
    @Query(
        "SELECT * FROM daily_times WHERE locationId = :locationId AND date <= :maxDate " +
            "ORDER BY date DESC LIMIT 1",
    )
    fun observeLatest(locationId: String, maxDate: String): Flow<DailyTimesEntity?>

    /** Housekeeping: drop rows older than the given date. */
    @Query("DELETE FROM daily_times WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)

    /** Housekeeping: drop future-dated rows left behind by a transiently wrong device clock. */
    @Query("DELETE FROM daily_times WHERE date > :afterDate")
    suspend fun deleteNewerThan(afterDate: String)
}
