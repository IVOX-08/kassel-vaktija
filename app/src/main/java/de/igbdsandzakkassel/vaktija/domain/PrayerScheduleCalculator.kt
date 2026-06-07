package de.igbdsandzakkassel.vaktija.domain

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import java.time.LocalDateTime

/** Result of evaluating the day's schedule against the current moment. */
data class ScheduleResult(
    /** The obligatory prayer currently in effect (its adhan passed, next hasn't). */
    val activePrayer: Prayer,
    /** The next upcoming obligatory prayer (wraps to tomorrow's Fajr after Isha). */
    val nextPrayer: Prayer,
    /** Exact moment of the next prayer's adhan (may be tomorrow). */
    val nextAdhan: LocalDateTime,
)

/**
 * Pure schedule logic. "Active" and "next" are computed over the five obligatory prayers;
 * Sunrise is shown on the dashboard but is neither active nor counted as next.
 *
 * TBD-decision: confirm with owner whether the countdown should also stop at Sunrise (Izlazak).
 * Current behaviour counts only to the five daily prayers.
 */
object PrayerScheduleCalculator {

    fun compute(times: DailyTimes, now: LocalDateTime): ScheduleResult {
        val today = now.toLocalDate()
        val schedule = Prayer.OBLIGATORY.map { it to LocalDateTime.of(today, times.adhan(it)) }

        val next = schedule.firstOrNull { it.second.isAfter(now) }
            ?: (Prayer.FAJR to LocalDateTime.of(today.plusDays(1), times.fajr))

        val active = schedule.lastOrNull { !it.second.isAfter(now) }?.first
            ?: Prayer.ISHA // before today's Fajr → still in the Isha period

        return ScheduleResult(activePrayer = active, nextPrayer = next.first, nextAdhan = next.second)
    }
}
