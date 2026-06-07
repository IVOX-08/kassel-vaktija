package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Locally-computed (uncalibrated) prayer times for one day of the month calendar. Calibration to
 * the official vaktija value is applied at read time, so these stay deterministic and cacheable.
 */
@Entity(tableName = "month_times")
data class MonthDayTimesEntity(
    @PrimaryKey val date: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
)
