package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MonthTimesDao {

    @Upsert
    suspend fun upsertAll(rows: List<MonthDayTimesEntity>)

    /** All cached rows for a month, e.g. monthPrefix = "2026-06%". */
    @Query("SELECT * FROM month_times WHERE date LIKE :monthPrefix ORDER BY date")
    suspend fun getMonth(monthPrefix: String): List<MonthDayTimesEntity>
}
