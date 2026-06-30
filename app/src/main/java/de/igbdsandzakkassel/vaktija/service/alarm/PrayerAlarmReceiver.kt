package de.igbdsandzakkassel.vaktija.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
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

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Foreground-service start / notification post must happen synchronously on the main thread.
        when (action) {
            ACTION_ADHAN -> {
                val prayer = prayerFrom(intent) ?: return
                val sound = AdhanSound.fromName(intent.getStringExtra(EXTRA_SOUND))
                val playWhenSilent = intent.getBooleanExtra(EXTRA_PLAY_WHEN_SILENT, false)
                val isJumua = intent.getBooleanExtra(EXTRA_IS_JUMUA, false)
                // How late this alarm is being delivered. A background-restricting OEM (e.g. Honor/
                // Huawei) can hold an exact alarm and only release it much later — typically when the
                // app is next opened — which would otherwise blast a long-overdue Adhan out loud. If
                // it's badly overdue we post a SILENT notice instead of playing the Adhan sound.
                val scheduledAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
                val stale = scheduledAt > 0L && System.currentTimeMillis() - scheduledAt > STALE_ADHAN_MS
                when {
                    stale -> {
                        PrayerNotifier.ensureChannels(context)
                        PrayerNotifier.postAdhanSilently(context, prayer, isJumua)
                    }
                    playWhenSilent || !isPhoneSilenced(context) -> {
                        // Play via the foreground service (won't be truncated); it also vibrates via the channel.
                        AdhanForegroundService.start(context, prayer, sound.name, isJumua)
                    }
                    else -> {
                        // Phone is on silent/vibrate and the user hasn't opted into overriding it: show a
                        // quiet prayer-time notice instead of playing the Adhan out loud (e.g. at work).
                        PrayerNotifier.ensureChannels(context)
                        PrayerNotifier.postAdhanSilently(context, prayer, isJumua)
                    }
                }
            }

            ACTION_PREWARN -> {
                val prayer = prayerFrom(intent) ?: return
                PrayerNotifier.ensureChannels(context)
                PrayerNotifier.postPreWarning(
                    context, prayer, intent.getIntExtra(EXTRA_MINUTES, 0),
                    intent.getBooleanExtra(EXTRA_IS_JUMUA, false),
                )
            }

            ACTION_WEEKLY_REMINDER -> PrayerNotifier.postWeeklyReminder(context)
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
                    ACTION_SILENCE_START -> {
                        dndController.silence()
                        // Record how long OUR DND lasts, so a lost END alarm can be recovered later.
                        settingsRepository.setDndActiveUntil(intent.getLongExtra(EXTRA_SILENCE_UNTIL, 0L))
                    }
                    ACTION_SILENCE_END -> {
                        dndController.restore()
                        settingsRepository.setDndActiveUntil(0L)
                    }
                    // Re-arm next week's reminder.
                    ACTION_WEEKLY_REMINDER -> alarmScheduler.rescheduleAll()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun prayerFrom(intent: Intent): Prayer? =
        Prayer.entries.getOrNull(intent.getIntExtra(EXTRA_PRAYER, -1))

    /** True if the phone's ringer is on silent or vibrate — then the Adhan should not play out loud. */
    private fun isPhoneSilenced(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    companion object {
        const val ACTION_ADHAN = "de.igbdsandzakkassel.vaktija.ALARM_ADHAN"
        const val ACTION_PREWARN = "de.igbdsandzakkassel.vaktija.ALARM_PREWARN"
        const val ACTION_SILENCE_START = "de.igbdsandzakkassel.vaktija.ALARM_SILENCE_START"
        const val ACTION_SILENCE_END = "de.igbdsandzakkassel.vaktija.ALARM_SILENCE_END"
        const val ACTION_WEEKLY_REMINDER = "de.igbdsandzakkassel.vaktija.ALARM_WEEKLY_REMINDER"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_PLAY_WHEN_SILENT = "extra_play_when_silent"
        const val EXTRA_IS_JUMUA = "extra_is_jumua"
        const val EXTRA_SILENCE_UNTIL = "extra_silence_until"
        const val EXTRA_TRIGGER_AT = "extra_trigger_at"

        /** An Adhan delivered more than this many ms after its scheduled time is treated as a missed/
         *  overdue alarm: post a silent notice instead of playing the Adhan out loud. 90 s covers
         *  normal Doze delivery jitter while catching the "fires on app-open, hours late" case. */
        private const val STALE_ADHAN_MS = 90_000L
    }
}
