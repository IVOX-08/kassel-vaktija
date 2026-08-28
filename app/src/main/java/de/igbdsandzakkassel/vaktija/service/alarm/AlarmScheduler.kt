package de.igbdsandzakkassel.vaktija.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.domain.PrayerWindows
import de.igbdsandzakkassel.vaktija.data.model.Prayer.Companion.silencesForCongregation
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.core.device.isLeanbackTv
import de.igbdsandzakkassel.vaktija.core.device.isTelevision
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import de.igbdsandzakkassel.vaktija.service.dnd.DndController
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact alarms for each enabled prayer's Adhan (+ optional pre-warning) for the rest of
 * today. Re-run on app open, after a data refresh, on settings change, on boot/time changes, and
 * after each alarm fires.
 *
 * Note: only today's times are available from the source, so alarms are scheduled per-day and
 * refreshed daily. (A midnight-precise self-reschedule is a Phase 9 refinement.)
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val ruleProvider: CommunityRuleProvider,
    private val dndController: DndController,
) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    // NonCancellable: rescheduling is a cancel-then-rearm sequence. If the caller's scope dies midway
    // (e.g. a settings toggle followed by a quick back-press cancelling the ViewModel scope), a plain
    // suspend body could cancel AFTER cancelAll but BEFORE re-arming — leaving ZERO alarms until the
    // next trigger. Wrapping the whole body guarantees the re-arm always completes once started.
    suspend fun rescheduleAll(): Unit = withContext(NonCancellable) {
        // Android TV is a passive wall-display board — never schedule Adhan / pre-warn / auto-silence
        // / weekly alarms (it must stay silent). Prayer-time DATA is refreshed separately by
        // VaktijaRefreshWorker before this call, so the board still updates. (FEATURE_LEANBACK is
        // reliable from this app context, unlike a UiModeManager reading.) Check both TV signals so a
        // device that shows the TV board can never still be scheduling alarms.
        if (context.isLeanbackTv() || context.isTelevision()) return@withContext
        val settings = settingsRepository.observe().first()
        // Read the times BEFORE cancelling: with a completely empty cache (fresh install before the
        // first fetch, cleared data) a stray TIME_SET/TIMEZONE broadcast must not wipe whatever is
        // already armed and then re-arm nothing.
        val times = timesRepository.observeToday().first()

        cancelAll()
        recoverStrandedDnd(settings.autoSilenceEnabled)

        // Weekly dhikr/hadith reminder — independent of prayer times, so arm it before the early
        // return below (which fires when today's times aren't cached yet).
        scheduleWeeklyReminder(settings.weeklyReminderEnabled)
        // Always re-arm the day-rollover alarm (self-perpetuating chain): shortly after midnight it
        // re-runs this method so the NEW day's alarms (above all Fajr) exist even if the user never
        // opens the app and the daily refresh worker only runs later in the day. Armed even when
        // times are missing, so an empty-cache night still retries once the cache fills.
        scheduleDayRollover()

        if (times == null) return@withContext
        // One-shot read of the ACTUAL configured rules (observeRules' first emission is the default).
        val rules = ruleProvider.getRules()

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY

        Prayer.NOTIFIABLE.forEach { prayer ->
            val prefs = settings.prefs(prayer)
            // On Friday the Dhuhr congregation IS the Jumu'ah prayer, held at the community jumua
            // time — so the Dhuhr slot's alarm, pre-notice and silence window all key off jumua.
            val isJumua = isFriday && prayer == Prayer.DHUHR
            val adhanAt =
                if (isJumua) LocalDateTime.of(today, rules.jumua)
                else LocalDateTime.of(today, times.adhan(prayer))

            // Adhan + pre-warning, per prayer. There is no master switch any more: whoever wants
            // a particular prayer silent turns that prayer off.
            if (prefs.enabled) {
                if (adhanAt.isAfter(now)) {
                    schedule(prayer, adhanAt, AlarmType.ADHAN, minutes = 0, sound = settings.sound, playWhenSilent = settings.playWhenSilent, isJumua = isJumua)
                }
                // Jumu'ah always gets a fixed 30-min pre-notice; other prayers use the per-prayer value.
                val preWarn = if (isJumua) JUMUA_PREWARN_MIN else prefs.preWarnMinutes
                if (preWarn > 0) {
                    val warnAt = adhanAt.minusMinutes(preWarn.toLong())
                    if (warnAt.isAfter(now)) {
                        schedule(prayer, warnAt, AlarmType.PREWARN, minutes = preWarn, sound = settings.sound, isJumua = isJumua)
                    }
                }
            }

            // The tracker's question, armed at the Iqamah — the moment the congregation prays,
            // and the moment the answer becomes true. It is NOT tied to the Adhan toggles: someone
            // who silenced the call to prayer because they are at work is exactly the person the
            // streak is for.
            if (prayer in Prayer.OBLIGATORY) {
                val askAt = PrayerWindows.windowFor(prayer, today, times, rules).opensAt
                if (askAt.isAfter(now)) {
                    schedule(prayer, askAt, AlarmType.TRACKER_ASK, minutes = 0, sound = settings.sound, isJumua = isJumua)
                }
            }

            // Auto-silence window: DND on BEFORE the Adhan, off again AFTER it. Jumu'ah ALWAYS silences
            // (10 min before → 40 min after) as a community default — it only actually takes effect if
            // the user granted Do-Not-Disturb access. Other prayers use the opt-in auto-silence setting.
            val silenceBefore = when {
                // Never silence the phone for sunrise: there is no congregation to be quiet for.
                !prayer.silencesForCongregation -> null
                isJumua -> JUMUA_SILENCE_BEFORE_MIN
                settings.autoSilenceEnabled -> settings.silenceBeforeMinutes
                else -> null
            }
            val silenceAfter = when {
                isJumua -> JUMUA_SILENCE_AFTER_MIN
                settings.autoSilenceEnabled -> settings.silenceMinutes
                else -> null
            }
            if (silenceBefore != null && silenceAfter != null) {
                val startAt = adhanAt.minusMinutes(silenceBefore.toLong())
                val endAt = adhanAt.plusMinutes(silenceAfter.toLong())
                val endMillis = endAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (startAt.isAfter(now)) {
                    // Carry the window's end on the START alarm, so the receiver records how long OUR
                    // DND should last and a lost END alarm can be recovered later (recoverStrandedDnd).
                    schedule(prayer, startAt, AlarmType.SILENCE_START, minutes = 0, sound = settings.sound, silenceUntilMillis = endMillis)
                }
                if (endAt.isAfter(now)) {
                    schedule(prayer, endAt, AlarmType.SILENCE_END, minutes = 0, sound = settings.sound)
                }
            }
        }
    }

    /**
     * If we turned Do-Not-Disturb on for an auto-silence window but its restore ("end") alarm was
     * lost — device powered off or force-stopped across the window, or the alarm dropped — turn it
     * back off now. Keyed on OUR own [SettingsRepository.getDndActiveUntil] watermark, so a DND the
     * user set themselves (which never writes that key) is never touched. Also restores immediately
     * when the user disables auto-silence MID-window (the END alarm was just cancelled by cancelAll
     * and, with the feature off, would never be re-armed — the phone would stay stranded in DND).
     */
    private suspend fun recoverStrandedDnd(autoSilenceEnabled: Boolean) {
        val until = settingsRepository.getDndActiveUntil()
        if (until <= 0L) return
        val expired = System.currentTimeMillis() > until
        if (expired || !autoSilenceEnabled) {
            dndController.restore()
            settingsRepository.setDndActiveUntil(0L)
        }
    }

    /** Arm the self-perpetuating shortly-after-midnight re-arm (see [rescheduleAll]). */
    private fun scheduleDayRollover() {
        val at = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 5))
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = dayRolloverPendingIntent()
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    private fun dayRolloverPendingIntent(): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(PrayerAlarmReceiver.ACTION_DAY_ROLLOVER)
        return PendingIntent.getBroadcast(
            context, DAY_ROLLOVER_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun schedule(
        prayer: Prayer,
        at: LocalDateTime,
        type: AlarmType,
        minutes: Int,
        sound: AdhanSound,
        playWhenSilent: Boolean = false,
        isJumua: Boolean = false,
        silenceUntilMillis: Long = 0L,
    ) {
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = pendingIntent(prayer, type, minutes, sound, playWhenSilent, isJumua, silenceUntilMillis, triggerAtMillis)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Exact-alarm permission revoked between check and use — fall back to inexact.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAll() {
        Prayer.NOTIFIABLE.forEach { prayer ->
            AlarmType.entries.forEach { type ->
                // Extras are ignored when matching a PendingIntent for cancellation, so any sound works.
                alarmManager.cancel(pendingIntent(prayer, type, 0, AdhanSound.DEFAULT, playWhenSilent = false, isJumua = false, silenceUntilMillis = 0L))
            }
        }
        // Also clear the weekly reminder + day-rollover here (both are re-armed right after) so
        // cancellation is centralized and no alarm can be orphaned.
        alarmManager.cancel(weeklyReminderPendingIntent())
        alarmManager.cancel(dayRolloverPendingIntent())
    }

    /** Arms (or cancels) the weekly Friday dhikr/hadith reminder. Re-armed each time it fires. */
    private fun scheduleWeeklyReminder(enabled: Boolean) {
        val pendingIntent = weeklyReminderPendingIntent()
        alarmManager.cancel(pendingIntent) // always clear first (handles the disabled case + re-arm)
        if (!enabled) return
        val at = nextFridayAt(WEEKLY_REMINDER_HOUR, 0)
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact (Doze-friendly) — a weekly reminder needs no second precision, so it neither needs
        // nor consumes the exact-alarm permission (reserved for prayer times).
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    /** The next Friday at [hour]:[minute], strictly in the future. */
    private fun nextFridayAt(hour: Int, minute: Int): LocalDateTime {
        val now = LocalDateTime.now()
        var date = now.toLocalDate()
        while (date.dayOfWeek != DayOfWeek.FRIDAY) date = date.plusDays(1)
        var dateTime = LocalDateTime.of(date, LocalTime.of(hour, minute))
        if (!dateTime.isAfter(now)) dateTime = dateTime.plusWeeks(1) // today is Friday but the time passed
        return dateTime
    }

    private fun weeklyReminderPendingIntent(): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(PrayerAlarmReceiver.ACTION_WEEKLY_REMINDER)
        return PendingIntent.getBroadcast(
            context, WEEKLY_REMINDER_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(
        prayer: Prayer,
        type: AlarmType,
        minutes: Int,
        sound: AdhanSound,
        playWhenSilent: Boolean,
        isJumua: Boolean,
        silenceUntilMillis: Long,
        triggerAtMillis: Long = 0L,
    ): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = type.action
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.ordinal)
            putExtra(PrayerAlarmReceiver.EXTRA_MINUTES, minutes)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND, sound.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PLAY_WHEN_SILENT, playWhenSilent)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_JUMUA, isJumua)
            putExtra(PrayerAlarmReceiver.EXTRA_SILENCE_UNTIL, silenceUntilMillis)
            putExtra(PrayerAlarmReceiver.EXTRA_TRIGGER_AT, triggerAtMillis)
        }
        val requestCode = prayer.ordinal * AlarmType.entries.size + type.ordinal
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private enum class AlarmType(val action: String) {
        ADHAN(PrayerAlarmReceiver.ACTION_ADHAN),
        PREWARN(PrayerAlarmReceiver.ACTION_PREWARN),
        SILENCE_START(PrayerAlarmReceiver.ACTION_SILENCE_START),
        SILENCE_END(PrayerAlarmReceiver.ACTION_SILENCE_END),
        TRACKER_ASK(PrayerAlarmReceiver.ACTION_TRACKER_ASK),
    }

    private companion object {
        // Unique request code, far above prayer alarm codes. Prayer codes = prayer.ordinal * types + type;
        // the Prayer enum includes SUNRISE so the obligatory ordinals reach ISHA=5 → max 5*4+3 = 23.
        const val WEEKLY_REMINDER_REQUEST = 9100
        const val DAY_ROLLOVER_REQUEST = 9200
        const val WEEKLY_REMINDER_HOUR = 11

        // Friday Jumu'ah, relative to the community jumua time: a 30-min pre-notice, and an auto-DND
        // window from 10 min before to 40 min after (silences phones for the khutbah + prayer).
        const val JUMUA_PREWARN_MIN = 30
        const val JUMUA_SILENCE_BEFORE_MIN = 10
        const val JUMUA_SILENCE_AFTER_MIN = 40
    }
}
