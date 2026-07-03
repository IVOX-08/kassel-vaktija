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

        // Pick by TIME, not by list position: on Fridays the Dhuhr slot carries the community
        // Jumu'ah time, which can be LATER than Asr (e.g. Jumu'ah 15:00 vs winter Asr 14:30) — a
        // positional scan would then skip Asr entirely.
        val next = schedule.filter { it.second.isAfter(now) }.minByOrNull { it.second }
            ?: (Prayer.FAJR to LocalDateTime.of(today.plusDays(1), times.fajr))

        val active = schedule.filter { !it.second.isAfter(now) }.maxByOrNull { it.second }?.first
            ?: Prayer.ISHA // before today's Fajr → still in the Isha period

        return ScheduleResult(activePrayer = active, nextPrayer = next.first, nextAdhan = next.second)
    }
}
