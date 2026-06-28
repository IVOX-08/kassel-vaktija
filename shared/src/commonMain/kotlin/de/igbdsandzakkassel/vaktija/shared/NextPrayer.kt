package de.igbdsandzakkassel.vaktija.shared

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** The upcoming prayer and how many minutes remain until it — ready for the UI. */
data class NextPrayerInfo(val name: String, val time: String, val inMinutes: Int)

/**
 * The next of the five daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) from now, in Kassel.
 * After Isha it wraps to tomorrow's Fajr. Sunrise is informational and not counted as a prayer.
 */
fun nextPrayerNow(): NextPrayerInfo {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    val calc = PrayerTimesCalculator()
    val todayT = calc.compute(today, tz)
    val tomorrowT = calc.compute(tomorrow, tz)

    // The five prayers today, then tomorrow's Fajr as the after-Isha wrap-around.
    val candidates = listOf(
        Triple("Fajr", today, todayT.fajr),
        Triple("Dhuhr", today, todayT.dhuhr),
        Triple("Asr", today, todayT.asr),
        Triple("Maghrib", today, todayT.maghrib),
        Triple("Isha", today, todayT.isha),
        Triple("Fajr", tomorrow, tomorrowT.fajr),
    )

    // The first whose moment is strictly after now (tomorrow's Fajr always qualifies).
    val next = candidates.first { (_, date, time) ->
        LocalDateTime(date, time).toInstant(tz) > now
    }
    val instant = LocalDateTime(next.second, next.third).toInstant(tz)
    val minutes = (instant - now).inWholeMinutes.toInt()
    return NextPrayerInfo(next.first, next.third.toHhMm(), minutes)
}
