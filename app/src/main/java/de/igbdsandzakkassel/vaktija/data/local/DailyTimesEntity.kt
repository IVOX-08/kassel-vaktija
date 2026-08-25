package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Entity

/**
 * Cached prayer times for one day AT ONE LOCATION. Times stored as "HH:mm"; date as ISO
 * "yyyy-MM-dd".
 *
 * The key is (locationId, date), not date alone: one community can span several towns whose times
 * differ, and a user switching town must not read the previous town's rows.
 */
@Entity(tableName = "daily_times", primaryKeys = ["locationId", "date"])
data class DailyTimesEntity(
    val locationId: String,
    val date: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fetchedAt: Long,
)
