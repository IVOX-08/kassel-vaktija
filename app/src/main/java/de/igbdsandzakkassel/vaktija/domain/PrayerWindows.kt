package de.igbdsandzakkassel.vaktija.domain

import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * When a prayer may still be answered.
 *
 * The tracker is only worth anything if the answer has to be given while it is true. A checklist
 * that can be ticked at midnight for the whole day is a memory game; this asks at the time and
 * closes the door when the time has passed.
 *
 *  - The window OPENS at the Iqamah, when the congregation actually prays.
 *  - It CLOSES when the next prayer's Adhan sounds — after that the prayer is late, and the app
 *    stops pretending otherwise.
 *
 * Fajr is the exception that makes the whole thing honest: its window closes at SUNRISE, not at
 * Dhuhr. Fajr's time genuinely ends there, and a window running until midday would let anyone tick
 * it over breakfast — exactly the shortcut the streak is meant to rule out.
 */
object PrayerWindows {

    data class Window(val opensAt: LocalDateTime, val closesAt: LocalDateTime) {
        fun contains(at: LocalDateTime): Boolean = !at.isBefore(opensAt) && at.isBefore(closesAt)
        fun hasClosed(at: LocalDateTime): Boolean = !at.isBefore(closesAt)
    }

    /**
     * The window for [prayer] on [date].
     *
     * On Friday the Dhuhr congregation IS Jumu'ah, held at the community's own time, so the window
     * opens then — the same substitution the dashboard and the alarms make.
     */
    fun windowFor(
        prayer: Prayer,
        date: LocalDate,
        times: DailyTimes,
        rules: CommunityRules,
    ): Window {
        val isJumua = date.dayOfWeek == DayOfWeek.FRIDAY && prayer == Prayer.DHUHR
        val opens = when {
            isJumua -> rules.jumua
            else -> rules.iqamah(prayer, times.adhan(prayer)) ?: times.adhan(prayer)
        }
        val closes = when (prayer) {
            // Fajr ends at sunrise. This is the rule the rest of the tracker leans on.
            Prayer.FAJR -> date.atTime(times.sunrise)
            Prayer.DHUHR -> date.atTime(times.asr)
            Prayer.ASR -> date.atTime(times.maghrib)
            Prayer.MAGHRIB -> date.atTime(times.isha)
            // Isha runs until the next Fajr. Tomorrow's times are usually not cached yet, and Fajr
            // shifts by about a minute a day, so today's Fajr moved on by one day is close enough
            // to the minute that matters here.
            Prayer.ISHA -> date.plusDays(1).atTime(times.fajr)
            Prayer.SUNRISE -> date.atTime(times.sunrise)
        }
        var opensAt = date.atTime(opens)
        // A community that sets its Iqamah past the next Adhan would otherwise produce a window
        // that never opens; clamp rather than silently drop the prayer from the tracker.
        if (!opensAt.isBefore(closes)) opensAt = closes.minusMinutes(1)
        return Window(opensAt, closes)
    }

    /** Every obligatory prayer's window for the day, in order. */
    fun allFor(date: LocalDate, times: DailyTimes, rules: CommunityRules): Map<Prayer, Window> =
        Prayer.OBLIGATORY.associateWith { windowFor(it, date, times, rules) }

    /** The prayer whose window is open at [at], if any. */
    fun openAt(
        at: LocalDateTime,
        date: LocalDate,
        times: DailyTimes,
        rules: CommunityRules,
    ): Prayer? = Prayer.OBLIGATORY.firstOrNull { windowFor(it, date, times, rules).contains(at) }
}
