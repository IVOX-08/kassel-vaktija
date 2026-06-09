package de.igbdsandzakkassel.vaktija.service.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.service.notification.PrayerNotifier

/**
 * Plays the Adhan when a prayer time arrives. A foreground service (type mediaPlayback) is used so
 * the full sound plays reliably even with the screen off — a notification sound would be cut short.
 * Stops itself when playback finishes or the user taps "Stop".
 */
class AdhanForegroundService : Service() {

    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stop()
            return START_NOT_STICKY
        }

        PrayerNotifier.ensureChannels(this)
        val prayer = Prayer.entries.getOrElse(intent?.getIntExtra(EXTRA_PRAYER, -1) ?: -1) { Prayer.FAJR }
        val sound = AdhanSound.fromName(intent?.getStringExtra(EXTRA_SOUND))

        startForegroundCompat(prayer)
        playSound(sound.rawResName ?: FALLBACK_SOUND)
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(prayer: Prayer) {
        val notification = PrayerNotifier.buildAdhanNotification(this, prayer, stopPendingIntent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                PrayerNotifier.ADHAN_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(PrayerNotifier.ADHAN_NOTIFICATION_ID, notification)
        }
    }

    private fun playSound(soundResName: String) {
        // If the selected Adhan file isn't bundled yet (e.g. adhan_short before the owner adds it),
        // fall back to the placeholder chime so something always plays. // TBD-asset:
        var resId = resources.getIdentifier(soundResName, "raw", packageName)
        if (resId == 0 && soundResName != FALLBACK_SOUND) {
            resId = resources.getIdentifier(FALLBACK_SOUND, "raw", packageName)
        }
        if (resId == 0) {
            stop()
            return
        }
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(
                    this@AdhanForegroundService,
                    Uri.parse("android.resource://$packageName/$resId"),
                )
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ -> stop(); true }
                prepare()
                start()
            }
        }.onFailure { stop() }
    }

    private fun stop() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopPendingIntent(): PendingIntent {
        val intent = Intent(this, AdhanForegroundService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "de.igbdsandzakkassel.vaktija.STOP_ADHAN"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_SOUND = "extra_sound"

        /** Bundled chime used when the selected Adhan audio isn't present yet. */
        private const val FALLBACK_SOUND = "adhan_placeholder"

        /** [soundName] is an [AdhanSound] enum name (resolved on the service side). */
        fun start(context: Context, prayer: Prayer, soundName: String) {
            val intent = Intent(context, AdhanForegroundService::class.java)
                .putExtra(EXTRA_PRAYER, prayer.ordinal)
                .putExtra(EXTRA_SOUND, soundName)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
