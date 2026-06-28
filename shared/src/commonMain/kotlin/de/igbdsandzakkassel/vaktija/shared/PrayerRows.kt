package de.igbdsandzakkassel.vaktija.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** One prayer's name and its "HH:MM" time — ready to display directly in any UI (incl. SwiftUI). */
data class PrayerRow(val name: String, val time: String)

/**
 * Today's six Kassel prayer rows, formatted as strings. Returns plain Strings on purpose so the
 * SwiftUI app can show them without wrestling with kotlinx-datetime types across the Kotlin↔Swift
 * bridge. This is the single function the iOS foundation screen calls.
 */
fun prayerRowsForToday(): List<PrayerRow> {
    val tz = TimeZone.currentSystemDefault()
    val t = PrayerTimesCalculator().compute(Clock.System.todayIn(tz), tz)
    return listOf(
        PrayerRow("Fajr", t.fajr.hhmm()),
        PrayerRow("Sunrise", t.sunrise.hhmm()),
        PrayerRow("Dhuhr", t.dhuhr.hhmm()),
        PrayerRow("Asr", t.asr.hhmm()),
        PrayerRow("Maghrib", t.maghrib.hhmm()),
        PrayerRow("Isha", t.isha.hhmm()),
    )
}

private fun LocalTime.hhmm(): String =
    hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
