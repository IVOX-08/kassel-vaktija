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
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmScheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
