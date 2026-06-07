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
        if (!settings.masterEnabled) return
        val times = timesRepository.observeToday().first() ?: return

        val now = LocalDateTime.now()
        val today = LocalDate.now()

        Prayer.OBLIGATORY.forEach { prayer ->
            val prefs = settings.prefs(prayer)
            if (!prefs.enabled) return@forEach

            val adhanAt = LocalDateTime.of(today, times.adhan(prayer))
            if (adhanAt.isAfter(now)) {
                schedule(prayer, adhanAt, preWarn = false, minutes = 0, sound = settings.sound)
            }
            if (prefs.preWarnMinutes > 0) {
                val warnAt = adhanAt.minusMinutes(prefs.preWarnMinutes.toLong())
                if (warnAt.isAfter(now)) {
                    schedule(prayer, warnAt, preWarn = true, minutes = prefs.preWarnMinutes, sound = settings.sound)
                }
            }
        }
    }

    private fun schedule(prayer: Prayer, at: LocalDateTime, preWarn: Boolean, minutes: Int, sound: AdhanSound) {
        val pendingIntent = pendingIntent(prayer, preWarn, minutes, sound)
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
            listOf(false, true).forEach { preWarn ->
                alarmManager.cancel(pendingIntent(prayer, preWarn, 0, AdhanSound.PLACEHOLDER))
            }
        }
    }

    private fun pendingIntent(prayer: Prayer, preWarn: Boolean, minutes: Int, sound: AdhanSound): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = if (preWarn) PrayerAlarmReceiver.ACTION_PREWARN else PrayerAlarmReceiver.ACTION_ADHAN
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayer.ordinal)
            putExtra(PrayerAlarmReceiver.EXTRA_MINUTES, minutes)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND, sound.rawResName)
        }
        val requestCode = prayer.ordinal * 2 + if (preWarn) 1 else 0
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
