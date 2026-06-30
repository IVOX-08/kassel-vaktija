package de.igbdsandzakkassel.vaktija.service.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.igbdsandzakkassel.vaktija.service.notification.NewsNotificationChecker
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver
import java.util.concurrent.TimeUnit

/**
 * Checks Firestore for a newly-posted announcement and notifies if there is one. Run as a proper
 * WorkManager job (not inline in a BroadcastReceiver) so the network fetch is not bound by the
 * ~10-second broadcast budget. Enqueued from the prayer-alarm receiver at each Adhan; the daily
 * refresh worker and app-open one-shot perform the same check directly.
 */
@HiltWorker
class NewsCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val newsChecker: NewsNotificationChecker,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        runCatching { newsChecker.checkAndNotify(applicationContext) }
        // Safety net for the home-screen widget: roll it to the current next prayer. The widget's own
        // exact "rollover" alarm can be killed by aggressive OEM battery savers (e.g. Honor/Huawei
        // power saving), which would otherwise leave it stuck on a past prayer counting into the
        // negative. This periodic refresh self-heals that within ~15 minutes even then.
        runCatching { PrayerTimesWidgetReceiver.refresh(applicationContext) }
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "news_check_now"
        private const val UNIQUE_PERIODIC = "news_check_periodic"

        private val networkOnly =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Run the check once, right now (e.g. on app open or when a prayer alarm fires). */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NewsCheckWorker>()
                    .setConstraints(networkOnly)
                    .build(),
            )
        }

        /**
         * Background safety net (no Firebase Blaze / FCM push needed): poll for new announcements and
         * Iqamah changes every 15 minutes (WorkManager's minimum). The watermark inside
         * [NewsNotificationChecker] dedupes, so this never double-notifies alongside the on-wake check
         * or a future FCM push. The OS may defer this in Doze, so it's "within ~15 min", not exact.
         */
        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NewsCheckWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(networkOnly)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                    .build(),
            )
        }
    }
}
