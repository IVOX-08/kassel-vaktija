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
import dagger.hilt.android.HiltAndroidApp
import de.igbdsandzakkassel.vaktija.core.device.isTelevision
import de.igbdsandzakkassel.vaktija.service.notification.NewsNotifier
import de.igbdsandzakkassel.vaktija.service.notification.PrayerNotifier
import de.igbdsandzakkassel.vaktija.service.notification.PushTopicSubscriber
import de.igbdsandzakkassel.vaktija.service.work.NewsCheckWorker
import de.igbdsandzakkassel.vaktija.service.work.StaleTimesWatcher
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

    @Inject
    lateinit var staleTimesWatcher: StaleTimesWatcher

    @Inject
    lateinit var pushTopicSubscriber: PushTopicSubscriber

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        installTvCrashRecovery()
        PrayerNotifier.ensureChannels(this)
        NewsNotifier.ensureChannel(this)
        // Subscribe for instant announcement pushes: one channel per community plus the
        // federation-wide one. No-op until a Cloud Function publishes to them (needs the Firebase
        // Blaze plan); until then the polling check below is the fallback.
        pushTopicSubscriber.start()
        schedulePrayerTimesRefresh()
        // No-billing fallback for announcement/Iqamah notifications: poll every ~15 min (plus the
        // on-wake check at each prayer alarm). Replaced by instant FCM push once Blaze is enabled.
        NewsCheckWorker.schedulePeriodic(this)
        // The TV board never restarts, so every start-tied refresh above fires only once in its
        // life. This watcher is what actually keeps it current day to day.
        staleTimesWatcher.start()
    }

    /**
     * The mosque's TV board runs 24/7 unattended: if an uncaught exception ever kills the process,
     * the entrance would show the Android TV home screen until someone finds the remote. Standard
     * kiosk pattern — schedule a relaunch of [MainActivity] ~2 s out, then let the process die.
     * Phones keep the normal crash behaviour (system dialog / silent restart by the user).
     */
    private fun installTvCrashRecovery() {
        if (!isTelevision()) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val intent = android.content.Intent(this, MainActivity::class.java)
                    .addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
                    )
                val pending = android.app.PendingIntent.getActivity(
                    this, 4242, intent,
                    android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
                val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.set(
                    android.app.AlarmManager.RTC,
                    System.currentTimeMillis() + 2_000,
                    pending,
                )
            }
            // Still crash "properly" so the system logs it (and any previous handler runs).
            previous?.uncaughtException(thread, throwable)
                ?: run {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(2)
                }
        }
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
            // UPDATE, not KEEP: existing installs are carrying the old (broken) constraints and
            // would otherwise keep them for the lifetime of the install.
            ExistingPeriodicWorkPolicy.UPDATE,
            // Every 6 h, not daily: a once-a-day job may land at any point inside its period, so
            // the board could sit on yesterday's times all morning. No batteryNotLow constraint —
            // a mains-powered TV has no battery to report, and an unsatisfiable constraint would
            // park this job forever.
            PeriodicWorkRequestBuilder<VaktijaRefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build(),
        )
    }
}
