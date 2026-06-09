package de.igbdsandzakkassel.vaktija.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.service.audio.AdhanForegroundService
import de.igbdsandzakkassel.vaktija.service.dnd.DndController
import de.igbdsandzakkassel.vaktija.service.notification.PrayerNotifier
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver
import de.igbdsandzakkassel.vaktija.service.work.NewsCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired by AlarmManager at a prayer time (Adhan), before it (pre-warning), or at the start/end of an
 * auto-silence window. Starts Adhan playback / posts a pre-warning / toggles Do Not Disturb, then
 * re-arms the remaining alarms.
 */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var dndController: DndController

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Foreground-service start / notification post must happen synchronously on the main thread.
        when (action) {
            ACTION_ADHAN -> {
                val prayer = prayerFrom(intent) ?: return
                val sound = AdhanSound.fromName(intent.getStringExtra(EXTRA_SOUND))
                // Play via the foreground service (won't be truncated); it also vibrates via the channel.
                AdhanForegroundService.start(context, prayer, sound.name)
            }

            ACTION_PREWARN -> {
                val prayer = prayerFrom(intent) ?: return
                PrayerNotifier.ensureChannels(context)
                PrayerNotifier.postPreWarning(context, prayer, intent.getIntExtra(EXTRA_MINUTES, 0))
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    // Re-arm subsequent alarms (today's remaining events) + advance the widget's
                    // "next prayer" highlight.
                    ACTION_ADHAN -> {
                        alarmScheduler.rescheduleAll()
                        PrayerTimesWidgetReceiver.refresh(context)
                        // Check for new announcements in a worker (the network fetch must not run
                        // inside the BroadcastReceiver's ~10s budget).
                        NewsCheckWorker.enqueue(context)
                    }
                    ACTION_PREWARN -> alarmScheduler.rescheduleAll()
                    ACTION_SILENCE_START -> dndController.silence()
                    ACTION_SILENCE_END -> dndController.restore()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun prayerFrom(intent: Intent): Prayer? =
        Prayer.entries.getOrNull(intent.getIntExtra(EXTRA_PRAYER, -1))

    companion object {
        const val ACTION_ADHAN = "de.igbdsandzakkassel.vaktija.ALARM_ADHAN"
        const val ACTION_PREWARN = "de.igbdsandzakkassel.vaktija.ALARM_PREWARN"
        const val ACTION_SILENCE_START = "de.igbdsandzakkassel.vaktija.ALARM_SILENCE_START"
        const val ACTION_SILENCE_END = "de.igbdsandzakkassel.vaktija.ALARM_SILENCE_END"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_SOUND = "extra_sound"
    }
}
