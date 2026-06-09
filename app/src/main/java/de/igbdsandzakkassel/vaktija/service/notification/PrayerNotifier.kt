package de.igbdsandzakkassel.vaktija.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import de.igbdsandzakkassel.vaktija.MainActivity
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer

/**
 * Notification channels + builders. The Adhan channel is silent because the audio is played by
 * [de.igbdsandzakkassel.vaktija.service.audio.AdhanForegroundService] via MediaPlayer (the system
 * would otherwise truncate a long sound and ignore it in silent ringer mode).
 */
object PrayerNotifier {

    const val CHANNEL_ADHAN = "prayer_adhan"
    const val CHANNEL_PREWARN = "prayer_prewarn"
    const val ADHAN_NOTIFICATION_ID = 1001
    private const val PREWARN_BASE_ID = 2000

    /** Two firm pulses, so the vibrate-only mode feels deliberate (wait, buzz, gap, buzz). */
    private val ADHAN_VIBRATION = longArrayOf(0, 400, 200, 400)

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val adhan = NotificationChannel(
            CHANNEL_ADHAN,
            context.getString(R.string.notif_channel_adhan),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_adhan_desc)
            setSound(null, null) // audio handled by the foreground service
            enableVibration(true)
            vibrationPattern = ADHAN_VIBRATION
        }
        val preWarn = NotificationChannel(
            CHANNEL_PREWARN,
            context.getString(R.string.notif_channel_prewarn),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_prewarn_desc)
        }
        manager.createNotificationChannel(adhan)
        manager.createNotificationChannel(preWarn)
    }

    /** High-priority Adhan notification shown by the foreground service while the sound plays. */
    fun buildAdhanNotification(
        context: Context,
        prayer: Prayer,
        stopIntent: PendingIntent,
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ADHAN)
        .setSmallIcon(R.drawable.ic_stat_adhan)
        .setLargeIcon(communityLogo(context))
        .setContentTitle(context.getString(R.string.notif_adhan_title, context.getString(prayer.labelRes)))
        .setContentText(context.getString(R.string.notif_adhan_text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setContentIntent(openAppIntent(context))
        .setOngoing(true)
        .addAction(0, context.getString(R.string.notif_stop), stopIntent)
        .build()

    /** Pre-warning notification ("Dhuhr in 10 min"). */
    fun postPreWarning(context: Context, prayer: Prayer, minutes: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_PREWARN)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(communityLogo(context))
            .setContentTitle(
                context.getString(R.string.notif_prewarn_title, context.getString(prayer.labelRes), minutes),
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

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The community coat-of-arms as the notification's large (colored) icon. */
    private fun communityLogo(context: Context) =
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_notification)
}
