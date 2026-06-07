package de.igbdsandzakkassel.vaktija.service.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.service.alarm.AlarmScheduler
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver

/**
 * Refreshes the cached prayer times from vaktija.eu. Scheduled daily (the source publishes one day
 * at a time) plus an expedited run on app open. Retries with backoff on failure. After a successful
 * refresh it re-arms the day's alarms with the latest times.
 */
@HiltWorker
class VaktijaRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PrayerTimesRepository,
    private val alarmScheduler: AlarmScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val refreshed = repository.refresh()
        alarmScheduler.rescheduleAll()
        PrayerTimesWidgetReceiver.refresh(applicationContext)
        return if (refreshed) Result.success() else Result.retry()
    }

    companion object {
        const val UNIQUE_PERIODIC = "vaktija_refresh_periodic"
        const val UNIQUE_ONESHOT = "vaktija_refresh_now"
    }
}
