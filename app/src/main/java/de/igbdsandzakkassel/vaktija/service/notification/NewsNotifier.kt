package de.igbdsandzakkassel.vaktija.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import de.igbdsandzakkassel.vaktija.MainActivity
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.settings.NewsSound
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import java.util.Locale

/**
 * Notifications for community announcements (the Firestore `news` collection). Uses its OWN channel
 * with a distinct rising "announcement" tone so it is clearly different from the prayer/Adhan alerts.
 * The notification text is shown in the user's selected app language.
 */
object NewsNotifier {

    // Bump the suffix whenever the channel's sound changes (a channel's sound is fixed at creation,
    // so a new id is the only way existing installs pick up a new tone). v2 = owner's glass-clink tone.
    const val CHANNEL_NEWS = "news_announcements_v2"
    private const val CHANNEL_NEWS_OLD = "news_announcements"
    const val NEWS_NOTIFICATION_ID = 3001

    // Distinct id so a "prayer times updated" notification never replaces an announcement (and a
    // later poll/push for the same change just updates this one in place, never stacking duplicates).
    const val CONFIG_NOTIFICATION_ID = 3002

    /** Short, light double-buzz — distinct from the Adhan channel's firmer pattern. */
    private val NEWS_VIBRATION = longArrayOf(0, 180, 120, 180)

    /**
     * The tone in force. Kept here because announcements are posted from three different places —
     * a worker, a push message and a config check — and threading the setting through all of them
     * would mean any path that forgot it would quietly post on the wrong channel.
     */
    @Volatile
    private var current: NewsSound = NewsSound.DEFAULT_SOUND

    /** Called when the setting is read or changed. */
    fun useSound(context: Context, sound: NewsSound) {
        current = sound
        ensureChannel(context, sound)
    }

    fun ensureChannel(context: Context, sound: NewsSound = current) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        // Drop the previous channels (old tones) so only the current ones remain.
        runCatching { manager.deleteNotificationChannel(CHANNEL_NEWS_OLD) }
        runCatching { manager.deleteNotificationChannel(CHANNEL_NEWS) }
        val raw = context.resources.getIdentifier(sound.rawResName, "raw", context.packageName)
        val channel = NotificationChannel(
            sound.channelId,
            context.getString(R.string.notif_channel_news),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_news_desc)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(Uri.parse("android.resource://${context.packageName}/$raw"), attrs)
            enableVibration(true)
            vibrationPattern = NEWS_VIBRATION
        }
        manager.createNotificationChannel(channel)
    }

    /** Post an announcement notification, localized to [lang] (the user's selected app language). */
    fun post(context: Context, item: NewsItem, lang: String) {
        ensureChannel(context, current)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        // Fallback title must be localized too — the raw context resolves the SYSTEM locale on a
        // background wake (Android 8-12), which would mix a Bosnian title with a translated body.
        val title = item.title(lang).ifBlank { localized(context, lang).getString(R.string.news_add) }
        val body = item.body(lang)
        val notification = NotificationCompat.Builder(context, current.channelId)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.logo_notification))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(NEWS_NOTIFICATION_ID, notification)
    }

    /** Post a raw announcement notification from plain strings (used by instant FCM pushes). */
    fun postRaw(context: Context, title: String, body: String) {
        ensureChannel(context, current)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val fallbackCtx = de.igbdsandzakkassel.vaktija.core.locale.LocaleController.persistedTag(context)
            ?.let { localized(context, it) } ?: context
        val notification = NotificationCompat.Builder(context, current.channelId)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.logo_notification))
            .setContentTitle(title.ifBlank { fallbackCtx.getString(R.string.news_add) })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(NEWS_NOTIFICATION_ID, notification)
    }

    /** Post a "prayer/Iqamah times were updated" notification, localized to [lang]. */
    fun postConfigUpdate(context: Context, lang: String) {
        ensureChannel(context, current)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val loc = localized(context, lang)
        val title = loc.getString(R.string.notif_config_title)
        val body = loc.getString(R.string.notif_config_body)
        val notification = NotificationCompat.Builder(context, current.channelId)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.logo_notification))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(CONFIG_NOTIFICATION_ID, notification)
    }

    /** A context whose resources resolve strings in [lang] (background wakes can't rely on the UI locale). */
    private fun localized(context: Context, lang: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang))
        return context.createConfigurationContext(config)
    }

    /** Opens the announcements, not the dashboard — that is what the notification was about. */
    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_NEWS, true)
        return PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
