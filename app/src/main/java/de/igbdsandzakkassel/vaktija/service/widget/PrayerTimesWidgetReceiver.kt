package de.igbdsandzakkassel.vaktija.service.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.content.getSystemService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.igbdsandzakkassel.vaktija.MainActivity
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Hilt access into the (non-component) widget provider. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerTimesRepository(): PrayerTimesRepository
}

/**
 * Home-screen widget showing the NEXT prayer's name + time and a live countdown (Chronometer) to it.
 *
 * Classic RemoteViews (not Glance) on purpose: a Chronometer ticks down on its own on the home
 * screen with no app updates. Each render also schedules a self-refresh just after that prayer's
 * time, so the widget automatically rolls to the following prayer. Refreshed too by the daily
 * worker and each Adhan alarm, and tapping it opens the app.
 */
class PrayerTimesWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshAsync(context)
    }

    private fun refreshAsync(context: Context) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                refresh(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "de.igbdsandzakkassel.vaktija.WIDGET_REFRESH"
        private const val ROLLOVER_REQUEST_CODE = 7710
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

        /** Re-render all placed widgets with the current next prayer + countdown. */
        suspend fun refresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, PrayerTimesWidgetReceiver::class.java))
            if (ids == null || ids.isEmpty()) return

            val repository = EntryPointAccessors
                .fromApplication(appContext, WidgetEntryPoint::class.java)
                .prayerTimesRepository()
            val times = repository.observeToday().first()

            val views = RemoteViews(appContext.packageName, R.layout.widget_next_prayer)
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(appContext))

            if (times == null) {
                views.setTextViewText(R.id.widget_prayer_name, "—")
                views.setTextViewText(R.id.widget_prayer_time, "")
                views.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime(), null, false)
            } else {
                val now = LocalDateTime.now()
                val (prayer, at) = nextPrayer(times, now)
                views.setTextViewText(R.id.widget_prayer_name, appContext.getString(prayer.labelRes))
                views.setTextViewText(
                    R.id.widget_prayer_time,
                    appContext.getString(R.string.widget_remaining) + "  ·  " + at.toLocalTime().format(TIME_FMT),
                )
                val millisUntil = Duration.between(now, at).toMillis().coerceAtLeast(0L)
                views.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime() + millisUntil, null, true)
                views.setChronometerCountDown(R.id.widget_chrono, true)
                scheduleRollover(appContext, at)
            }
            manager.updateAppWidget(ids, views)
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /** Next obligatory prayer today; if all have passed, tomorrow's Fajr (today's time is fine). */
        private fun nextPrayer(times: DailyTimes, now: LocalDateTime): Pair<Prayer, LocalDateTime> {
            val today = now.toLocalDate()
            for (prayer in Prayer.OBLIGATORY) {
                val at = LocalDateTime.of(today, times.adhan(prayer))
                if (at.isAfter(now)) return prayer to at
            }
            return Prayer.FAJR to LocalDateTime.of(today.plusDays(1), times.fajr)
        }

        /** Schedule a self-refresh just after [at] so the widget rolls forward to the next prayer. */
        private fun scheduleRollover(context: Context, at: LocalDateTime) {
            val alarmManager = context.getSystemService<AlarmManager>() ?: return
            val intent = Intent(context, PrayerTimesWidgetReceiver::class.java).setAction(ACTION_REFRESH)
            val pending = PendingIntent.getBroadcast(
                context,
                ROLLOVER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val triggerAt = at.plusSeconds(2).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            try {
                val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }
}
