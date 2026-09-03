package de.igbdsandzakkassel.vaktija.shared

import com.batoulapps.adhan2.Coordinates
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** One prayer's name and its "HH:MM" time — ready to display directly in any UI (incl. SwiftUI). */
data class PrayerRow(val name: String, val time: String)

/**
 * Today's six prayer rows for the given position, formatted as strings. Returns plain Strings on purpose so the
 * SwiftUI app can show them without wrestling with kotlinx-datetime types across the Kotlin↔Swift
 * bridge.
 */
fun prayerRowsForToday(
    latitude: Double = PrayerTimesCalculator.KASSEL_LAT,
    longitude: Double = PrayerTimesCalculator.KASSEL_LNG,
): List<PrayerRow> {
    val tz = TimeZone.currentSystemDefault()
    val t = PrayerTimesCalculator(Coordinates(latitude, longitude))
        .compute(Clock.System.todayIn(tz), tz)
    return listOf(
        PrayerRow("Fajr", t.fajr.toHhMm()),
        PrayerRow("Sunrise", t.sunrise.toHhMm()),
        PrayerRow("Dhuhr", t.dhuhr.toHhMm()),
        PrayerRow("Asr", t.asr.toHhMm()),
        PrayerRow("Maghrib", t.maghrib.toHhMm()),
        PrayerRow("Isha", t.isha.toHhMm()),
    )
}

/** "HH:MM", zero-padded. Shared by the UI-facing helpers in this module. */
internal fun LocalTime.toHhMm(): String =
    hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
