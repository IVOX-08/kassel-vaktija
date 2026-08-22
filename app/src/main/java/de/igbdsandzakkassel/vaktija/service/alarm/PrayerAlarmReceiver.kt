package de.igbdsandzakkassel.vaktija.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
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

    @Inject
    lateinit var timesRepository: PrayerTimesRepository

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
                // If OUR OWN auto-silence / Jumu'ah DND window is active right now, the phones in the
                // mosque were silenced BY THIS APP for the khutbah/prayer — blasting the Adhan out
                // loud through it (USAGE_ALARM pierces DND) would defeat the whole feature at the
                // worst possible moment. Show the quiet notice instead.
                val inOwnSilenceWindow = runCatching {
                    kotlinx.coroutines.runBlocking { settingsRepository.getDndActiveUntil() } >
                        System.currentTimeMillis()
                }.getOrDefault(false)
                val ringerMode = ringerMode(context)
                when {
                    stale || inOwnSilenceWindow -> {
                        PrayerNotifier.ensureChannels(context)
                        PrayerNotifier.postAdhanSilently(context, prayer, isJumua)
                    }
                    playWhenSilent || ringerMode == AudioManager.RINGER_MODE_NORMAL -> {
                        // Play via the foreground service (won't be truncated). Defensive: if the OS
                        // rejects the background FGS start (OEM restriction), degrade to the quiet
                        // notice instead of dropping the prayer notice entirely.
                        runCatching { AdhanForegroundService.start(context, prayer, sound.name, isJumua) }
                            .onFailure {
                                PrayerNotifier.ensureChannels(context)
                                PrayerNotifier.postAdhanSilently(context, prayer, isJumua)
                            }
                    }
                    ringerMode == AudioManager.RINGER_MODE_VIBRATE -> {
                        // Phone on vibrate → no sound, but it MUST buzz. Channel vibration can't be
                        // relied on (locked once created; some OEMs drop it), so buzz explicitly.
                        PrayerNotifier.ensureChannels(context)
                        PrayerNotifier.postAdhanSilently(context, prayer, isJumua)
                        vibrateAdhan(context)
                    }
                    else -> {
                        // Phone fully on silent (no vibrate): a quiet prayer-time notice only.
                        PrayerNotifier.ensureChannels(context)
                        PrayerNotifier.postAdhanSilently(context, prayer, isJumua)
                    }
                }
            }

            ACTION_PREWARN -> {
                val prayer = prayerFrom(intent) ?: return
                // Same staleness gate as the Adhan: an OEM-held alarm released hours late must not
                // post a factually wrong "prayer in N minutes" notice.
                val warnScheduledAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
                val warnStale =
                    warnScheduledAt > 0L && System.currentTimeMillis() - warnScheduledAt > STALE_ADHAN_MS
                if (!warnStale) {
                    PrayerNotifier.ensureChannels(context)
                    PrayerNotifier.postPreWarning(
                        context, prayer, intent.getIntExtra(EXTRA_MINUTES, 0),
                        intent.getBooleanExtra(EXTRA_IS_JUMUA, false),
                    )
                }
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
                        // Only lift DND if WE turned it on (our watermark is set). A stray END alarm
                        // (e.g. delivered after the user disabled auto-silence, or when START never
                        // ran) must not force-disable a Do-Not-Disturb the user set themselves.
                        if (settingsRepository.getDndActiveUntil() > 0L) {
                            dndController.restore()
                            settingsRepository.setDndActiveUntil(0L)
                        }
                    }
                    // Shortly-after-midnight self re-arm: plan the NEW day's alarms (above all Fajr)
                    // and roll the widget to the new day.
                    ACTION_DAY_ROLLOVER -> {
                        // Fetch the NEW day's times FIRST — vaktija.eu publishes one day at a time,
                        // so without this the alarms below (and the board, and the widget) would all
                        // be re-armed from yesterday's cached values. If the fetch fails here (the
                        // mosque Wi-Fi may not be up at 00:05) StaleTimesWatcher keeps retrying.
                        runCatching { timesRepository.refresh() }
                        alarmScheduler.rescheduleAll()
                        PrayerTimesWidgetReceiver.refresh(context)
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

    private fun ringerMode(context: Context): Int =
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode
            ?: AudioManager.RINGER_MODE_NORMAL

    /**
     * Fire the Adhan buzz directly. Uses ALARM usage so it is delivered even on vibrate mode and
     * through Do-Not-Disturb, and so it works regardless of the (locked, OEM-flaky) channel vibration.
     */
    private fun vibrateAdhan(context: Context) {
        val pattern = longArrayOf(0, 400, 200, 400) // wait, buzz, gap, buzz
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        runCatching { vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), attrs) }
    }

    companion object {
        const val ACTION_ADHAN = "de.igbdsandzakkassel.vaktija.ALARM_ADHAN"
        const val ACTION_PREWARN = "de.igbdsandzakkassel.vaktija.ALARM_PREWARN"
        const val ACTION_SILENCE_START = "de.igbdsandzakkassel.vaktija.ALARM_SILENCE_START"
        const val ACTION_SILENCE_END = "de.igbdsandzakkassel.vaktija.ALARM_SILENCE_END"
        const val ACTION_WEEKLY_REMINDER = "de.igbdsandzakkassel.vaktija.ALARM_WEEKLY_REMINDER"
        const val ACTION_DAY_ROLLOVER = "de.igbdsandzakkassel.vaktija.ALARM_DAY_ROLLOVER"
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
