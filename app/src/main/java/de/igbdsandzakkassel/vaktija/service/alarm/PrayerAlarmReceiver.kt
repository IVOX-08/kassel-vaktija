package de.igbdsandzakkassel.vaktija.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.service.audio.AdhanForegroundService
import de.igbdsandzakkassel.vaktija.service.notification.PrayerNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired by AlarmManager at a prayer time (Adhan) or before it (pre-warning). Starts the Adhan
 * playback service or posts a pre-warning notification, then re-arms the remaining alarms.
 */
@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.entries.getOrElse(intent.getIntExtra(EXTRA_PRAYER, -1)) { return }

        when (intent.action) {
            ACTION_ADHAN -> {
                val sound = intent.getStringExtra(EXTRA_SOUND) ?: "adhan_placeholder"
                AdhanForegroundService.start(context, prayer, sound)
            }

            ACTION_PREWARN -> {
                PrayerNotifier.ensureChannels(context)
                PrayerNotifier.postPreWarning(context, prayer, intent.getIntExtra(EXTRA_MINUTES, 0))
            }
        }

        // Re-arm subsequent alarms (today's remaining events).
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmScheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADHAN = "de.igbdsandzakkassel.vaktija.ALARM_ADHAN"
        const val ACTION_PREWARN = "de.igbdsandzakkassel.vaktija.ALARM_PREWARN"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_SOUND = "extra_sound"
    }
}
