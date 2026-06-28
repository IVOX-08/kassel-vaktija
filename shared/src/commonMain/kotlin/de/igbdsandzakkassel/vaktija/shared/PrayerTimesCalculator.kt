package de.igbdsandzakkassel.vaktija.shared

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Computes Kassel prayer times with the adhan2 library, using the EXACT same parameters as the
 * Android app: base method Muslim World League, Hanafi madhab (Bosniak community; affects Asr).
 *
 * Pure Kotlin + multiplatform (adhan2 + kotlinx-datetime), so it compiles unchanged for the JVM
 * (tests), Android and iOS.
 */
class PrayerTimesCalculator(
    private val coordinates: Coordinates = Coordinates(KASSEL_LAT, KASSEL_LNG),
) {
    private val parameters =
        CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(madhab = Madhab.HANAFI)

    /** Prayer times for [date], expressed in [timeZone] (defaults to the device's current zone). */
    fun compute(
        date: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): DailyPrayerTimes {
        val components = DateComponents(date.year, date.monthNumber, date.dayOfMonth)
        val times = PrayerTimes(coordinates, components, parameters)
        return DailyPrayerTimes(
            date = date,
            fajr = times.fajr.toLocalTime(timeZone),
            sunrise = times.sunrise.toLocalTime(timeZone),
            dhuhr = times.dhuhr.toLocalTime(timeZone),
            asr = times.asr.toLocalTime(timeZone),
            maghrib = times.maghrib.toLocalTime(timeZone),
            isha = times.isha.toLocalTime(timeZone),
        )
    }

    /** Every day of [month] (1–12) in [year], each with its prayer times — for the month calendar. */
    fun month(
        year: Int,
        month: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<DailyPrayerTimes> {
        val first = LocalDate(year, month, 1)
        val dayCount = first.daysUntil(first.plus(1, DateTimeUnit.MONTH))
        return (0 until dayCount).map { compute(first.plus(it, DateTimeUnit.DAY), timeZone) }
    }

    private fun Instant.toLocalTime(timeZone: TimeZone): LocalTime =
        toLocalDateTime(timeZone).time

    companion object {
        const val KASSEL_LAT: Double = 51.3127
        const val KASSEL_LNG: Double = 9.4797
    }
}
