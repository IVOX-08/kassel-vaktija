package de.igbdsandzakkassel.vaktija.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * The six daily prayer-time moments for one calendar day, as local wall-clock times.
 *
 * Pure Kotlin + kotlinx-datetime, so it lives in commonMain and is shared by Android and iOS.
 * (The Android app has its own `DailyTimes` model built on java.time; this shared one intentionally
 * uses kotlinx-datetime, which works on every platform including iOS where java.time is absent.)
 */
data class DailyPrayerTimes(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
)
