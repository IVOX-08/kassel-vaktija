package de.igbdsandzakkassel.vaktija.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms alarms after events that invalidate them: device boot, app update, and time/timezone/
 * locale changes. (Alarms don't survive reboots, so this is essential.)
 */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        // The receiver has to be exported -- BOOT_COMPLETED comes from the system and would not
        // reach it otherwise -- so any app on the phone can also send it an intent. Checking the
        // action is what keeps that from meaning anything: an intent with no action, or a made-up
        // one, is dropped here.
        //
        // Nothing terrible was reachable (the worst a stranger could do is make us re-arm alarms we
        // were going to re-arm anyway), but a receiver that acts on whatever arrives is a habit
        // that ages badly -- the day something heavier hangs off it, the check is already missing.
        if (intent.action !in HANDLED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmScheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        /** Exactly the actions declared in the manifest, and nothing else. Keep the two in step. */
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
