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
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.tracker.PrayerLogRepository
import de.igbdsandzakkassel.vaktija.domain.PrayerWindows
import de.igbdsandzakkassel.vaktija.service.tracker.TrackerAnswerReceiver
import java.time.LocalDate
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Hilt access into the (non-component) widget provider. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerTimesRepository(): PrayerTimesRepository
    fun communityRuleProvider(): CommunityRuleProvider
    fun prayerLogRepository(): PrayerLogRepository
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

            val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
            val times = entryPoint.prayerTimesRepository().observeToday().first()
            // Friday: the Dhuhr congregation IS Jumu'ah at the community time — the widget must count
            // to the SAME moment the dashboard and the Adhan alarm use, not the raw Dhuhr time.
            val jumua = runCatching { entryPoint.communityRuleProvider().getRules().jumua }.getOrNull()

            // Render strings in the user's CHOSEN app language: below Android 13 a bare app context
            // resolves the SYSTEM locale, so localize it via the persisted language tag.
            val locCtx = LocaleController.persistedTag(appContext)?.let { tag ->
                val config = android.content.res.Configuration(appContext.resources.configuration)
                config.setLocale(Locale.forLanguageTag(tag))
                appContext.createConfigurationContext(config)
            } ?: appContext

            val views = RemoteViews(appContext.packageName, R.layout.widget_next_prayer)
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(appContext))

            if (times == null) {
                views.setTextViewText(R.id.widget_prayer_name, "—")
                views.setTextViewText(R.id.widget_prayer_time, "")
                views.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime(), null, false)
            } else {
                val now = LocalDateTime.now()
                val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
                val effective = if (isFriday && jumua != null) times.copy(dhuhr = jumua) else times
                val (prayer, at) = nextPrayer(effective, now)
                val labelRes =
                    if (isFriday && jumua != null && prayer == Prayer.DHUHR) R.string.prayer_jumua
                    else prayer.labelRes
                views.setTextViewText(R.id.widget_prayer_name, locCtx.getString(labelRes))
                views.setTextViewText(
                    R.id.widget_prayer_time,
                    locCtx.getString(R.string.widget_remaining) + "  ·  " + at.toLocalTime().format(TIME_FMT),
                )
                // Zone-aware difference: across a DST changeover night a naive LocalDateTime
                // difference is off by exactly 1 h (the Chronometer would count negative for an hour).
                val zone = ZoneId.systemDefault()
                val millisUntil =
                    Duration.between(ZonedDateTime.now(zone), at.atZone(zone)).toMillis().coerceAtLeast(0L)
                views.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime() + millisUntil, null, true)
                views.setChronometerCountDown(R.id.widget_chrono, true)
                scheduleRollover(appContext, at)
            }

            renderTracker(appContext, locCtx, views, times, entryPoint)
            manager.updateAppWidget(ids, views)
        }

        /**
         * The tracker strip: the open question with its two buttons, or the streak.
         *
         * Answering here is the point. The home screen is already in front of the reader, so the
         * answer costs one tap and no unlock; a question that first needs the app opened is a
         * question that waits until the window has closed.
         */
        private suspend fun renderTracker(
            context: Context,
            locCtx: Context,
            views: RemoteViews,
            times: DailyTimes?,
            entryPoint: WidgetEntryPoint,
        ) {
            val log = entryPoint.prayerLogRepository()
            val today = LocalDate.now()
            val rules = runCatching { entryPoint.communityRuleProvider().getRules() }.getOrNull()
            val open = if (times == null || rules == null) {
                null
            } else {
                PrayerWindows.openAt(LocalDateTime.now(), today, times, rules)
                    ?.takeIf { !log.isAnswered(today, it) }
            }

            if (open != null) {
                views.setTextViewText(
                    R.id.widget_tracker_text,
                    locCtx.getString(R.string.tracker_ask_title, locCtx.getString(open.labelRes)),
                )
                views.setViewVisibility(R.id.widget_tracker_yes, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_tracker_no, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_tracker_yes, locCtx.getString(R.string.action_yes))
                views.setTextViewText(R.id.widget_tracker_no, locCtx.getString(R.string.action_no))
                views.setOnClickPendingIntent(
                    R.id.widget_tracker_yes,
                    answerIntent(context, open, today, true),
                )
                views.setOnClickPendingIntent(
                    R.id.widget_tracker_no,
                    answerIntent(context, open, today, false),
                )
            } else {
                val streak = log.getStreak(today)
                val done = log.getDay(today).prayedCount
                views.setTextViewText(
                    R.id.widget_tracker_text,
                    "🔥 " + streak + "  \u00B7  " + done + " / 5",
                )
                views.setViewVisibility(R.id.widget_tracker_yes, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_tracker_no, android.view.View.GONE)
            }
        }

        private fun answerIntent(
            context: Context,
            prayer: Prayer,
            date: LocalDate,
            prayed: Boolean,
        ): PendingIntent {
            val intent = Intent(context, TrackerAnswerReceiver::class.java).apply {
                action = TrackerAnswerReceiver.ACTION_ANSWER
                putExtra(TrackerAnswerReceiver.EXTRA_PRAYER, prayer.name)
                putExtra(TrackerAnswerReceiver.EXTRA_DATE, date.toString())
                putExtra(TrackerAnswerReceiver.EXTRA_PRAYED, prayed)
            }
            // Its own request-code space, apart from the notification's, so a widget tap and a
            // shade tap cannot overwrite one another's intent.
            val code = 7300 + prayer.ordinal * 2 + if (prayed) 1 else 0
            return PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
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
