package de.igbdsandzakkassel.vaktija.shared

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Computes the daily prayer times for Kassel with the adhan2 library, using the EXACT same
 * parameters as the Android app: base method Muslim World League, Hanafi madhab (the Bosniak
 * community; the madhab affects the Asr time).
 *
 * This is the first piece of real business logic shared between Android and iOS. Both adhan2 and
 * kotlinx-datetime are Kotlin Multiplatform, so this file compiles unchanged for the JVM (tests),
 * Android, and iOS.
 *
 * Note: this mirrors `data/calendar/PrayerTimeCalculator.kt` from the Android app, but ported off
 * java.time (Android/JVM-only) onto kotlinx-datetime so it runs on iOS too.
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

    private fun Instant.toLocalTime(timeZone: TimeZone): LocalTime =
        toLocalDateTime(timeZone).time

    companion object {
        const val KASSEL_LAT: Double = 51.3127
        const val KASSEL_LNG: Double = 9.4797
    }
}
