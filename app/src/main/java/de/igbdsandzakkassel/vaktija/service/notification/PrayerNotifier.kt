package de.igbdsandzakkassel.vaktija.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import de.igbdsandzakkassel.vaktija.MainActivity
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import java.util.Locale

/**
 * Notification channels + builders. The Adhan channel is silent because the audio is played by
 * [de.igbdsandzakkassel.vaktija.service.audio.AdhanForegroundService] via MediaPlayer (the system
 * would otherwise truncate a long sound and ignore it in silent ringer mode).
 */
object PrayerNotifier {

    // v2: channel-level vibration proved unreliable — a channel's vibration is LOCKED once created
    // (so an app update can't add it) and some OEMs (e.g. Honor) silently drop it. The buzz is now
    // fired explicitly from PrayerAlarmReceiver (alarm usage), so this channel stays fully silent.
    const val CHANNEL_ADHAN = "prayer_adhan_v2"
    const val CHANNEL_PREWARN = "prayer_prewarn_v2"
    const val CHANNEL_REMINDER = "weekly_reminder_v2"
    const val ADHAN_NOTIFICATION_ID = 1001
    private const val PREWARN_BASE_ID = 2000
    private const val REMINDER_NOTIFICATION_ID = 4001

    fun ensureChannels(context: Context) {
        val loc = loc(context)
        val manager = context.getSystemService<NotificationManager>() ?: return
        // Remove the old v1 Adhan channel whose (locked) vibration setting some devices ignored.
        manager.deleteNotificationChannel("prayer_adhan")
        // An existing channel keeps the importance it was created with, so raising these to
        // urgent means new ids and clearing the old ones out of the system's list.
        runCatching { manager.deleteNotificationChannel("prayer_prewarn") }
        runCatching { manager.deleteNotificationChannel("weekly_reminder") }
        val adhan = NotificationChannel(
            CHANNEL_ADHAN,
            loc.getString(R.string.notif_channel_adhan),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = loc.getString(R.string.notif_channel_adhan_desc)
            setSound(null, null) // audio handled by the foreground service
            enableVibration(false) // vibration is fired explicitly (alarm usage) from the receiver
        }
        val preWarn = NotificationChannel(
            CHANNEL_PREWARN,
            loc.getString(R.string.notif_channel_prewarn),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = loc.getString(R.string.notif_channel_prewarn_desc)
        }
        // Gentle weekly reminder — its own channel with the system default tone (distinct from the
        // Adhan and announcement sounds).
        val reminder = NotificationChannel(
            CHANNEL_REMINDER,
            loc.getString(R.string.notif_channel_reminder),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = loc.getString(R.string.notif_channel_reminder_desc)
            enableVibration(true)
        }
        manager.createNotificationChannel(adhan)
        manager.createNotificationChannel(preWarn)
        manager.createNotificationChannel(reminder)
    }

    /** High-priority Adhan notification shown by the foreground service while the sound plays. */
    /**
     * [showStop] is true only for the recorded Adhan. That one runs for a minute or more, and
     * someone at work or in a lecture needs a way to cut it short. The plain tones are over in a
     * couple of seconds — a Stop button under them would be a button nobody could reach in time.
     */
    fun buildAdhanNotification(
        context: Context,
        prayer: Prayer,
        stopIntent: PendingIntent,
        isJumua: Boolean = false,
        showStop: Boolean = true,
    ): Notification {
        val loc = loc(context)
        return NotificationCompat.Builder(context, CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(communityLogo(context))
            .setContentTitle(loc.getString(R.string.notif_adhan_title, prayerLabel(loc, prayer, isJumua)))
            .setContentText(loc.getString(R.string.notif_adhan_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .apply { if (showStop) addAction(0, loc.getString(R.string.notif_stop), stopIntent) }
            .build()
    }

    /**
     * Quiet prayer-time notice used when the phone is muted/on vibrate and the user hasn't opted into
     * playing the Adhan out loud. The channel is silent; on vibrate mode the caller
     * ([de.igbdsandzakkassel.vaktija.service.alarm.PrayerAlarmReceiver]) also fires an explicit buzz.
     */
    fun postAdhanSilently(context: Context, prayer: Prayer, isJumua: Boolean = false) {
        val loc = loc(context)
        if (!hasNotificationPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(communityLogo(context))
            .setContentTitle(loc.getString(R.string.notif_adhan_title, prayerLabel(loc, prayer, isJumua)))
            .setContentText(loc.getString(R.string.notif_adhan_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(ADHAN_NOTIFICATION_ID, notification)
    }

    /** Pre-warning notification ("Dhuhr in 10 min" / "Jumu'ah in 30 min"). */
    fun postPreWarning(context: Context, prayer: Prayer, minutes: Int, isJumua: Boolean = false) {
        val loc = loc(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_PREWARN)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(communityLogo(context))
            .setContentTitle(
                loc.getString(R.string.notif_prewarn_title, prayerLabel(loc, prayer, isJumua), minutes),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        if (hasNotificationPermission(context)) {
            NotificationManagerCompat.from(context).notify(PREWARN_BASE_ID + prayer.ordinal, notification)
        }
    }

    /** Gentle weekly reminder to read some dhikr and a hadith. */
    fun postWeeklyReminder(context: Context) {
        val loc = loc(context)
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return
        val body = loc.getString(R.string.notif_reminder_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(communityLogo(context))
            .setContentTitle(loc.getString(R.string.notif_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }

    /**
     * The context whose language these notifications are written in.
     *
     * NOT simply `context`. On Android 13 and newer the system applies the per-app language to
     * every context, so the plain one happens to be right; below 13 the AndroidX backport only
     * reaches Activities, and a broadcast receiver would fall back to the app's BASE resources --
     * which are Bosnian. A user on Android 8 to 12 who chose German would have been getting the
     * Adhan and the pre-warning in Bosnian, and minSdk here is 26.
     *
     * The language itself comes from [LocaleController.resolvedTag], which never guesses -- see the
     * comment there, and the tracker bug that made it necessary.
     */
    private fun loc(context: Context): Context =
        LocaleController.resolvedTag(context)
            ?.let { tag ->
                val config = Configuration(context.resources.configuration)
                config.setLocale(Locale.forLanguageTag(tag))
                context.createConfigurationContext(config)
            }
            ?: context

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * The prayer's display name — "Jumu'ah" for the Friday Dhuhr slot, otherwise the normal label.
     * Takes the ALREADY localized context; every caller passes its own `loc`.
     */
    private fun prayerLabel(loc: Context, prayer: Prayer, isJumua: Boolean): String =
        loc.getString(if (isJumua) R.string.prayer_jumua else prayer.labelRes)

    private fun hasNotificationPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The community coat-of-arms as the notification's large (colored) icon. */
    private fun communityLogo(context: Context) =
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_notification)
}
