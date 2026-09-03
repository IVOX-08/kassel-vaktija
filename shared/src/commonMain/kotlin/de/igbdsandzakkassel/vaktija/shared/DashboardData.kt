package de.igbdsandzakkassel.vaktija.shared

import com.batoulapps.adhan2.Coordinates
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** One dashboard row: a prayer with its Adhan and (optional) Iqamah time, both "HH:MM". */
data class DashboardRow(val name: String, val adhan: String, val iqamah: String?)

/**
 * Today's prayer rows with the community's confirmed Iqamah rule (mirrors the Android defaults in
 * `data/model/CommunityRules.kt`): Fajr Iqamah is a fixed congregation time (04:30); Dhuhr +10,
 * Asr +10, Maghrib +5, Isha +0 minutes after the Adhan; Sunrise has no Iqamah.
 *
 * [latitude]/[longitude] sind die Koordinaten der GEWAEHLTEN Gemeinde. Ohne sie rechnete diese
 * Funktion fuer jede der 81 Gemeinden Kassel — offline, im Monatskalender und in den geplanten
 * Meldungen der kommenden Tage. Der Standard bleibt Kassel, damit bestehende Aufrufer weiter
 * uebersetzen; beide Apps sollen ihn NICHT benutzen.
 */
fun dashboardRowsForToday(
    latitude: Double = PrayerTimesCalculator.KASSEL_LAT,
    longitude: Double = PrayerTimesCalculator.KASSEL_LNG,
): List<DashboardRow> {
    val tz = TimeZone.currentSystemDefault()
    val t = PrayerTimesCalculator(Coordinates(latitude, longitude))
        .compute(Clock.System.todayIn(tz), tz)
    return listOf(
        // Fajr Iqamah: fixed community time. Spec default 04:30 — the real value comes from
        // Firestore config/community.fajrIqamah once Firebase is wired (then it overrides this).
        DashboardRow("Fajr", t.fajr.toHhMm(), LocalTime(4, 30).toHhMm()),
        DashboardRow("Sunrise", t.sunrise.toHhMm(), null),
        DashboardRow("Dhuhr", t.dhuhr.toHhMm(), t.dhuhr.plusMinutes(10).toHhMm()),
        DashboardRow("Asr", t.asr.toHhMm(), t.asr.plusMinutes(10).toHhMm()),
        DashboardRow("Maghrib", t.maghrib.toHhMm(), t.maghrib.plusMinutes(5).toHhMm()),
        DashboardRow("Isha", t.isha.toHhMm(), t.isha.toHhMm()),
    )
}

/** Adds [minutes] to a wall-clock time, wrapping at midnight. */
private fun LocalTime.plusMinutes(minutes: Int): LocalTime {
    val total = (hour * 60 + minute + minutes).mod(24 * 60)
    return LocalTime(total / 60, total % 60)
}
