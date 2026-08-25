package de.igbdsandzakkassel.vaktija.data.calendar

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import kotlinx.datetime.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant as JavaInstant

/**
 * Computes prayer times locally for any date using the adhan2 library. vaktija.eu only publishes
 * today, so the month calendar is computed here and then calibrated against today's official value
 * (see [de.igbdsandzakkassel.vaktija.data.repository.MonthCalendarRepository]).
 *
 * Bosniak community → Hanafi madhab (affects Asr). Base method is Muslim World League; the per-prayer
 * calibration offset absorbs the remaining difference from the official vaktija figures.
 */
@Singleton
class PrayerTimeCalculator @Inject constructor() {

    private val parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(madhab = Madhab.HANAFI)

    fun compute(date: LocalDate, latitude: Double, longitude: Double): DailyTimes {
        val coordinates = Coordinates(latitude, longitude)
        val components = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val times = PrayerTimes(coordinates, components, parameters)
        return DailyTimes(
            date = date,
            fajr = times.fajr.toLocalTime(),
            sunrise = times.sunrise.toLocalTime(),
            dhuhr = times.dhuhr.toLocalTime(),
            asr = times.asr.toLocalTime(),
            maghrib = times.maghrib.toLocalTime(),
            isha = times.isha.toLocalTime(),
        )
    }

    private fun Instant.toLocalTime(): LocalTime =
        JavaInstant.ofEpochMilli(toEpochMilliseconds()).atZone(ZoneId.systemDefault()).toLocalTime()

    private companion object {
        const val KASSEL_LAT = 51.3127
        const val KASSEL_LNG = 9.4797
    }
}
