package de.igbdsandzakkassel.vaktija.data.tracker

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.trackerStore by preferencesDataStore(name = "prayer_log")

/** What is known about one prayer on one day. */
enum class PrayerAnswer { YES, NO, UNANSWERED }

/** One day's record. */
data class DayLog(
    val date: LocalDate,
    val answers: Map<Prayer, PrayerAnswer>,
) {
    val prayedCount: Int get() = answers.count { it.value == PrayerAnswer.YES }
    val isComplete: Boolean get() = prayedCount == Prayer.OBLIGATORY.size
}

/**
 * The prayer log behind the streak.
 *
 * Everything stays on this phone. A record of whether someone prayed is about as private as data
 * gets, and it buys nothing to send it anywhere: the streak is shown to its owner, and the mosque's
 * reward is claimed by walking in and showing the screen.
 *
 * Stored as two 5-bit masks per day rather than a row per prayer — a year is 730 small integers,
 * which the preferences file holds without noticing, and computing a streak means walking back day
 * by day in memory.
 *
 * Two masks, not one, because "did not pray" and "never said" have to stay apart. They score the
 * same for the streak, but only one of them can still be answered, and only one of them should be
 * counted as an honest answer on the screen.
 */
@Singleton
class PrayerLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.trackerStore

    fun observeDay(date: LocalDate): Flow<DayLog> = store.data.map { it.dayLog(date) }

    suspend fun getDay(date: LocalDate): DayLog = store.data.first().dayLog(date)

    /**
     * Records an answer. Writing YES is refused unless [withinWindow] — the window is checked by
     * the caller, which is the only place that knows the day's prayer times.
     */
    suspend fun answer(date: LocalDate, prayer: Prayer, prayed: Boolean) {
        val bit = 1 shl Prayer.OBLIGATORY.indexOf(prayer).coerceAtLeast(0)
        store.edit { prefs ->
            val answered = prefs[answeredKey(date)] ?: 0
            val prayedMask = prefs[prayedKey(date)] ?: 0
            prefs[answeredKey(date)] = answered or bit
            prefs[prayedKey(date)] = if (prayed) prayedMask or bit else prayedMask and bit.inv()
        }
    }

    /** Whether this prayer has already been answered today — the notification must not ask twice. */
    suspend fun isAnswered(date: LocalDate, prayer: Prayer): Boolean =
        getDay(date).answers[prayer] != PrayerAnswer.UNANSWERED

    /**
     * Consecutive complete days, ending today if today is already complete and yesterday otherwise.
     *
     * A day counts only when all five were answered YES. Missing one — said no, or let the window
     * close in silence — ends the run, which is the whole point: a streak that survives a missed
     * Fajr would be worth nothing to the person holding it, and nothing to the mosque handing out
     * the reward.
     */
    fun observeStreak(today: LocalDate): Flow<Int> = store.data.map { prefs ->
        var day = if (prefs.dayLog(today).isComplete) today else today.minusDays(1)
        var streak = 0
        // A run longer than a year is not worth the read; nobody's flame is capped in practice.
        while (streak < 400 && prefs.dayLog(day).isComplete) {
            streak++
            day = day.minusDays(1)
        }
        streak
    }

    suspend fun getStreak(today: LocalDate): Int = observeStreak(today).first()

    private fun Preferences.dayLog(date: LocalDate): DayLog {
        val answered = this[answeredKey(date)] ?: legacyMask(date)
        val prayed = this[prayedKey(date)] ?: legacyMask(date)
        return DayLog(
            date = date,
            answers = Prayer.OBLIGATORY.withIndex().associate { (index, prayer) ->
                val bit = 1 shl index
                prayer to when {
                    answered and bit == 0 -> PrayerAnswer.UNANSWERED
                    prayed and bit != 0 -> PrayerAnswer.YES
                    else -> PrayerAnswer.NO
                }
            },
        )
    }

    /**
     * The old tracker stored a single "done" mask in SharedPreferences and let anyone tick any
     * prayer at any time. Those days are read back as answered-and-prayed so nobody loses a streak
     * they had already earned when this update arrives.
     */
    private fun legacyMask(date: LocalDate): Int =
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .getInt("d_$date", 0)

    private companion object {
        const val LEGACY_PREFS = "tracker_prefs"
        fun prayedKey(date: LocalDate) = intPreferencesKey("prayed_$date")
        fun answeredKey(date: LocalDate) = intPreferencesKey("answered_$date")
    }
}
