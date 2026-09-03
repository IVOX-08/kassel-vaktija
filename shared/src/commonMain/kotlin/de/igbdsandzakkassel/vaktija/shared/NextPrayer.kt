package de.igbdsandzakkassel.vaktija.shared

import com.batoulapps.adhan2.Coordinates
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** The upcoming prayer plus minutes and seconds remaining — ready for the UI (incl. a live countdown). */
data class NextPrayerInfo(
    val name: String,
    val time: String,
    val inMinutes: Int,
    val inSeconds: Int,
)

/**
 * The next of the five daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) from now.
 * After Isha it wraps to tomorrow's Fajr. Sunrise is informational and not counted as a prayer.
 *
 * [latitude]/[longitude]: die Koordinaten der gewaehlten Gemeinde.
 */
fun nextPrayerNow(
    latitude: Double = PrayerTimesCalculator.KASSEL_LAT,
    longitude: Double = PrayerTimesCalculator.KASSEL_LNG,
): NextPrayerInfo {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val today = now.toLocalDateTime(tz).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    val calc = PrayerTimesCalculator(Coordinates(latitude, longitude))
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
    val remaining = LocalDateTime(next.second, next.third).toInstant(tz) - now
    return NextPrayerInfo(
        name = next.first,
        time = next.third.toHhMm(),
        inMinutes = remaining.inWholeMinutes.toInt(),
        inSeconds = remaining.inWholeSeconds.toInt(),
    )
}
