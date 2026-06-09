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
    /** Auto-silence (Do Not Disturb) the phone around each prayer time. */
    val autoSilenceEnabled: Boolean = false,
    /** How long the phone stays silenced after each prayer's Adhan time. */
    val silenceMinutes: Int = 15,
    val perPrayer: Map<Prayer, PrayerAlarmPrefs> = Prayer.OBLIGATORY.associateWith { PrayerAlarmPrefs() },
) {
    fun prefs(prayer: Prayer): PrayerAlarmPrefs = perPrayer[prayer] ?: PrayerAlarmPrefs()

    companion object {
        /** Pre-warning options offered in the UI (minutes before Adhan; 0 = exactly at Adhan). */
        val PRE_WARN_OPTIONS = listOf(0, 5, 10, 15, 30)

        /** Auto-silence duration options (minutes after the Adhan). */
        val SILENCE_OPTIONS = listOf(10, 15, 20, 30)
    }
}
