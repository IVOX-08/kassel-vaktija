package de.igbdsandzakkassel.vaktija.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
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
) {
    private val alarmManager = context.getSystemService<AlarmManager>()!!

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    suspend fun rescheduleAll() {
        cancelAll()
        val settings = settingsRepository.observe().first()
        val times = timesRepository.observeToday().first() ?: return

        val now = LocalDateTime.now()
        val today = LocalDate.now()

        Prayer.OBLIGATORY.forEach { prayer ->
            val prefs = settings.prefs(prayer)
            val adhanAt = LocalDateTime.of(today, times.adhan(prayer))

            // Adhan + pre-warning (gated by the notifications master toggle + per-prayer enable).
            if (settings.masterEnabled && prefs.enabled) {
                if (adhanAt.isAfter(now)) {
                    schedule(prayer, adhanAt, AlarmType.ADHAN, minutes = 0, sound = settings.sound)
                }
                if (prefs.preWarnMinutes > 0) {
                    val warnAt = adhanAt.minusMinutes(prefs.preWarnMinutes.toLong())
                    if (warnAt.isAfter(now)) {
                        schedule(prayer, warnAt, AlarmType.PREWARN, minutes = prefs.preWarnMinutes, sound = settings.sound)
                    }
                }
            }

            // Auto-silence window (independent of the notifications toggle): DND on at the Adhan,
            // off again after the configured duration.
            if (settings.autoSilenceEnabled) {
                if (adhanAt.isAfter(now)) {
                    schedule(prayer, adhanAt, AlarmType.SILENCE_START, minutes = 0, sound = settings.sound)
                }
                val endAt = adhanAt.plusMinutes(settings.silenceMinutes.toLong())
                if (endAt.isAfter(now)) {
                    schedule(prayer, endAt, AlarmType.SILENCE_END, minutes = 0, sound = settings.sound)
                }
            }
        }
    }

    private fun schedule(prayer: Prayer, at: LocalDateTime, type: AlarmType, minutes: Int, sound: AdhanSound) {
        val pendingIntent = pendingIntent(prayer, type, minutes, sound)
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
        Prayer.OBLIGATORY.forEach { prayer ->
            AlarmType.entries.forEach { type ->
                // Extras are ignored when matching a PendingIntent for cancellation, so any sound works.
                alarmManager.cancel(pendingIntent(prayer, type, 0, AdhanSound.DEFAULT))
            }
        }
    }

    private fun pendingIntent(prayer: Prayer, type: AlarmType, minutes: Int, sound: AdhanSound): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = type.action
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.ordinal)
            putExtra(PrayerAlarmReceiver.EXTRA_MINUTES, minutes)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND, sound.name)
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
    }
}
