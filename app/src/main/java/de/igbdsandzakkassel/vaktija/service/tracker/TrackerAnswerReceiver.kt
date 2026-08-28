package de.igbdsandzakkassel.vaktija.service.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.tracker.PrayerLogRepository
import de.igbdsandzakkassel.vaktija.domain.PrayerWindows
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Records the answer to "Did you pray X?" — from the notification's buttons or the widget.
 *
 * The window is re-checked here rather than trusted from the notification. A notification can sit
 * in the shade for hours; without this check, tapping "Yes" on a Fajr question at noon would count,
 * and the streak would be worth nothing. A late "Yes" is simply dropped — the day stays broken, as
 * it already was.
 */
@AndroidEntryPoint
class TrackerAnswerReceiver : BroadcastReceiver() {

    @Inject lateinit var logRepository: PrayerLogRepository
    @Inject lateinit var timesRepository: PrayerTimesRepository
    @Inject lateinit var ruleProvider: CommunityRuleProvider

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ANSWER) return
        val prayer = runCatching {
            Prayer.valueOf(intent.getStringExtra(EXTRA_PRAYER).orEmpty())
        }.getOrNull() ?: return
        val date = runCatching {
            LocalDate.parse(intent.getStringExtra(EXTRA_DATE).orEmpty())
        }.getOrNull() ?: LocalDate.now()
        val prayed = intent.getBooleanExtra(EXTRA_PRAYED, false)

        // Take the question down at once, whatever the outcome — leaving it up after a tap looks
        // like the tap was lost.
        TrackerNotifier.cancel(context, prayer)

        val pending = goAsync()
        scope.launch {
            try {
                val today = runCatching { timesRepository.observeToday().first() }.getOrNull()
                val rules = ruleProvider.getRules()
                val open = today != null && PrayerWindows
                    .windowFor(prayer, date, today, rules)
                    .contains(LocalDateTime.now())
                // "No" is always recorded: an honest no is information, and it is what breaks the
                // streak on purpose. "Yes" only counts inside the window.
                if (!prayed || open) {
                    logRepository.answer(date, prayer, prayed && open)
                }
                PrayerTimesWidgetReceiver.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ANSWER = "de.igbdsandzakkassel.vaktija.TRACKER_ANSWER"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_DATE = "date"
        const val EXTRA_PRAYED = "prayed"
    }
}
