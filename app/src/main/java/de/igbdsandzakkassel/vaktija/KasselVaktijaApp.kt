package de.igbdsandzakkassel.vaktija

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import de.igbdsandzakkassel.vaktija.service.notification.NewsNotifier
import de.igbdsandzakkassel.vaktija.service.notification.PrayerNotifier
import de.igbdsandzakkassel.vaktija.service.work.VaktijaRefreshWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application entry point + Hilt root. Also provides the WorkManager configuration (so workers can
 * be Hilt-injected) and schedules the prayer-times refresh.
 */
@HiltAndroidApp
class KasselVaktijaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        PrayerNotifier.ensureChannels(this)
        NewsNotifier.ensureChannel(this)
        // Subscribe for instant announcement pushes. No-op until a Cloud Function publishes to the
        // "announcements" topic (needs the Firebase Blaze plan); the poll-on-wake check is the fallback.
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic("announcements") }
        schedulePrayerTimesRefresh()
    }

    private fun schedulePrayerTimesRefresh() {
        val workManager = WorkManager.getInstance(this)

        // Refresh once now (keeps today's times current when the app is opened).
        workManager.enqueueUniqueWork(
            VaktijaRefreshWorker.UNIQUE_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<VaktijaRefreshWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build(),
        )

        // Daily background refresh (vaktija.eu publishes one day at a time).
        workManager.enqueueUniquePeriodicWork(
            VaktijaRefreshWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<VaktijaRefreshWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build(),
        )
    }
}
