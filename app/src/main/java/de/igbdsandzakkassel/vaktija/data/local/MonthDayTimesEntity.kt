package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Entity

/**
 * Locally-computed (uncalibrated) prayer times for one day of the month calendar, AT ONE LOCATION.
 * Calibration to the official vaktija value is applied at read time, so these stay deterministic
 * and cacheable.
 *
 * Keyed by (locationId, date) for the same reason as the daily cache: the towns inside one
 * community sit far enough apart to compute different times.
 */
@Entity(tableName = "month_times", primaryKeys = ["locationId", "date"])
data class MonthDayTimesEntity(
    val locationId: String,
    val date: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
)
