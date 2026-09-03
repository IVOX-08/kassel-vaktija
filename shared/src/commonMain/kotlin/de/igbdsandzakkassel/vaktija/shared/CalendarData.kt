package de.igbdsandzakkassel.vaktija.shared

import com.batoulapps.adhan2.Coordinates
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** One row of the month calendar: a day number + weekday + the six prayer times, all "HH:MM". */
data class CalendarDay(
    val day: Int,
    val weekday: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val isToday: Boolean,
)

/**
 * Every day of [month] (1–12) in [year] with its prayer times — for the calendar screen.
 *
 * [latitude]/[longitude]: die Koordinaten der gewaehlten Gemeinde. Der Monatskalender ist der
 * Bildschirm, auf dem eine falsche Position am deutlichsten auffaellt — er zeigt dreissig Tage
 * auf einmal, und zwischen Hamburg und Muenchen laufen die Kurven ueber einen Monat sichtbar
 * auseinander.
 */
fun monthForDisplay(
    year: Int,
    month: Int,
    latitude: Double = PrayerTimesCalculator.KASSEL_LAT,
    longitude: Double = PrayerTimesCalculator.KASSEL_LNG,
): List<CalendarDay> {
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.todayIn(tz)
    return PrayerTimesCalculator(Coordinates(latitude, longitude)).month(year, month, tz).map { d ->
        CalendarDay(
            day = d.date.dayOfMonth,
            weekday = weekdayShort(d.date.dayOfWeek),
            fajr = d.fajr.toHhMm(),
            sunrise = d.sunrise.toHhMm(),
            dhuhr = d.dhuhr.toHhMm(),
            asr = d.asr.toHhMm(),
            maghrib = d.maghrib.toHhMm(),
            isha = d.isha.toHhMm(),
            isToday = d.date == today,
        )
    }
}

private fun weekdayShort(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Di"
    DayOfWeek.WEDNESDAY -> "Mi"
    DayOfWeek.THURSDAY -> "Do"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "So"
}
