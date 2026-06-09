package de.igbdsandzakkassel.vaktija.service.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.igbdsandzakkassel.vaktija.service.notification.NewsNotificationChecker

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
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "news_check_now"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NewsCheckWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    )
                    .build(),
            )
        }
    }
}
