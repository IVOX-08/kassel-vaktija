package de.igbdsandzakkassel.vaktija.data.settings

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer

/**
 * What plays when a prayer time arrives.
 *
 * - [rawResName] is the `res/raw` audio file to play (via the foreground service), or `null` for a
 *   vibrate-only alert that needs no long audio.
 * - [vibrate] is whether the phone vibrates for this mode.
 *
 * The full/short Adhan audio files (`adhan_full`, `adhan_short`) are dropped into `res/raw` by the
 * owner; until then the playback engine falls back to the bundled `adhan_placeholder` chime, so
 * every option works immediately. // TBD-asset: real adhan_full.mp3 / adhan_short.mp3
 */
enum class AdhanSound(
    @param:StringRes val labelRes: Int,
    val rawResName: String?,
    val vibrate: Boolean,
) {
    FULL_ADHAN(R.string.sound_full_adhan, "adhan_full", true),
    SHORT_ADHAN(R.string.sound_short_adhan, "adhan_short", true),
    CHIME(R.string.sound_chime, "chime", true),
    VIBRATE_ONLY(R.string.sound_vibrate_only, null, true),
    ;

    /** True if this mode plays an audio file (vs. a vibrate-only alert). */
    val hasAudio: Boolean get() = rawResName != null

    companion object {
        val DEFAULT = SHORT_ADHAN

        /** Resolve a persisted/intent enum name, defaulting safely on any mismatch. */
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
