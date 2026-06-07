package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached prayer times for one day. Times stored as "HH:mm"; date as ISO "yyyy-MM-dd". */
@Entity(tableName = "daily_times")
data class DailyTimesEntity(
    @PrimaryKey val date: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fetchedAt: Long,
)
