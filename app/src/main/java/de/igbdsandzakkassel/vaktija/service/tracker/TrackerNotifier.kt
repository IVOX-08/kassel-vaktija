package de.igbdsandzakkassel.vaktija.service.tracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import de.igbdsandzakkassel.vaktija.MainActivity
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import java.time.LocalDate
import java.util.Locale

/**
 * "Did you pray Fajr?" — asked once per prayer, answered from the notification itself.
 *
 * The question is the whole feature. A tracker that waits to be opened is a tracker nobody opens;
 * a question that arrives with two buttons is answered in the second it takes to read it, which is
 * the only way a streak survives contact with a working day.
 *
 * Its own channel, and deliberately quiet: five notifications a day that each make a sound would
 * be the fastest way to get the app muted altogether, and the Adhan already made the noise that
 * matters a few minutes earlier.
 */
object TrackerNotifier {

    const val CHANNEL_TRACKER = "prayer_tracker_v2"
    private const val BASE_ID = 5100

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        runCatching { manager.deleteNotificationChannel("prayer_tracker") }
        val channel = NotificationChannel(
            CHANNEL_TRACKER,
            context.getString(R.string.notif_channel_tracker),
            // Urgent, at the owner's instruction: the question exists to be answered, and one that
            // waits silently in the shade until the window has closed answers nothing.
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_tracker_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** Asks about [prayer], with Yes and No as actions. [lang] is the user's chosen app language. */
    fun ask(context: Context, prayer: Prayer, date: LocalDate, lang: String?) {
        if (!hasPermission(context)) return
        // A receiver can run in a process where the Application class has not created channels yet.
        ensureChannel(context)
        val loc = lang?.let { localized(context, it) } ?: context
        val name = loc.getString(prayer.labelRes)

        val notification = NotificationCompat.Builder(context, CHANNEL_TRACKER)
            .setSmallIcon(R.drawable.ic_stat_adhan)
            .setContentTitle(loc.getString(R.string.tracker_ask_title, name))
            .setContentText(loc.getString(R.string.tracker_ask_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .addAction(0, loc.getString(R.string.action_yes), answerIntent(context, prayer, date, true))
            .addAction(0, loc.getString(R.string.action_no), answerIntent(context, prayer, date, false))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(idFor(prayer), notification)
        }
    }

    /** Takes the question down — the window closed, or it was answered somewhere else. */
    fun cancel(context: Context, prayer: Prayer) {
        runCatching { NotificationManagerCompat.from(context).cancel(idFor(prayer)) }
    }

    private fun idFor(prayer: Prayer) = BASE_ID + prayer.ordinal

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
        // Distinct request code per prayer AND answer, or the two buttons would share one intent
        // and both would record whichever was created last.
        val requestCode = prayer.ordinal * 2 + if (prayed) 1 else 0
        return PendingIntent.getBroadcast(
            context,
            BASE_ID + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        BASE_ID,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRACKER, true)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun hasPermission(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * The app language, not the phone's. A background receiver resolves the SYSTEM locale, so
     * someone who set the app to Bosnian on a German phone would be asked in German.
     */
    private fun localized(context: Context, lang: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang))
        return context.createConfigurationContext(config)
    }
}
