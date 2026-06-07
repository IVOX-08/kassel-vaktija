package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {

    @Upsert
    suspend fun upsert(entity: DailyTimesEntity)

    @Query("SELECT * FROM daily_times WHERE date = :date LIMIT 1")
    fun observe(date: String): Flow<DailyTimesEntity?>

    /** Most recently dated row — used as an offline fallback when today isn't cached yet. */
    @Query("SELECT * FROM daily_times ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<DailyTimesEntity?>

    /** Housekeeping: drop rows older than the given date. */
    @Query("DELETE FROM daily_times WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)
}
