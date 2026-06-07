package de.igbdsandzakkassel.vaktija.data.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Adhan times for a single day (from vaktija.eu in Phase 1; placeholder source for now).
 * Fajr may be overridden by the community; Jumua/Eid are community-set.
 */
data class DailyTimes(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val jumua: LocalTime? = null,
    val eid: LocalTime? = null,
) {
    fun adhan(prayer: Prayer): LocalTime = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}
