package de.igbdsandzakkassel.vaktija.data.settings

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer

/**
 * Sound played when a prayer time arrives ([rawResName] = the `res/raw` audio file, played via the
 * foreground service). The phone also vibrates (the Adhan channel carries a vibration pattern), so
 * vibration always accompanies the sound rather than being a standalone option.
 */
enum class AdhanSound(
    @param:StringRes val labelRes: Int,
    val rawResName: String,
) {
    SHORT_ADHAN(R.string.sound_short_adhan, "adhan_short"),
    CHIME(R.string.sound_chime, "chime"),
    ;

    companion object {
        val DEFAULT = SHORT_ADHAN

        /** Resolve a persisted/intent enum name, defaulting safely on any mismatch (incl. removed values). */
        fun fromName(name: String?): AdhanSound =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}

/** Per-prayer alarm preferences. */
data class PrayerAlarmPrefs(
    val enabled: Boolean = true,
    val preWarnMinutes: Int = 0,
)

/** All alarm-related settings (per-prayer enable + pre-warning; global master toggle + sound). */
data class AlarmSettings(
    val masterEnabled: Boolean = true,
    val sound: AdhanSound = AdhanSound.DEFAULT,
    /**
     * Whether to play the Adhan out loud even when the phone's ringer is on silent/vibrate. Off by
     * default, so muting the phone (e.g. at work) also silences the Adhan — only a quiet notification
     * is shown. Turn on to make the Adhan behave like an alarm that overrides silent mode.
     */
    val playWhenSilent: Boolean = false,
    /** Auto-silence (Do Not Disturb) the phone around each prayer time. */
    val autoSilenceEnabled: Boolean = false,
    /** How long the phone is silenced BEFORE each prayer's Adhan time. */
    val silenceBeforeMinutes: Int = 5,
    /** How long the phone stays silenced AFTER each prayer's Adhan time. */
    val silenceMinutes: Int = 10,
    /** A gentle weekly reminder (Friday) to read some dhikr and a hadith. */
    val weeklyReminderEnabled: Boolean = true,
    val perPrayer: Map<Prayer, PrayerAlarmPrefs> = Prayer.NOTIFIABLE.associateWith { prayer ->
        // Sunrise is off by default: it is a reminder that Fajr is running out, wanted by some and
        // an unexpected early alarm for everyone else.
        PrayerAlarmPrefs(enabled = prayer != Prayer.SUNRISE)
    },
) {
    fun prefs(prayer: Prayer): PrayerAlarmPrefs = perPrayer[prayer] ?: PrayerAlarmPrefs()

    companion object {
        /** Pre-warning options offered in the UI (minutes before Adhan; 0 = exactly at Adhan). */
        val PRE_WARN_OPTIONS = listOf(0, 5, 10, 15, 30)

        /**
         * Minutes BEFORE sunrise for the Fajr-is-ending reminder. Includes 0 at the owner's
         * request — a notice exactly at sunrise still tells you the window has closed.
         */
        // Coarser and reaching further back than the other prayers: the point at sunrise is
        // "Fajr is running out", and someone who wants an hour's warning wants a whole hour, not
        // a choice between 25 and 30 minutes.
        val SUNRISE_WARN_OPTIONS = listOf(0, 10, 20, 30, 40, 50, 60)

        /** Auto-silence duration options (minutes before/after the Adhan). */
        val SILENCE_OPTIONS = listOf(5, 10, 15, 20, 30)
    }
}
