package de.igbdsandzakkassel.vaktija.data.settings

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer

/**
 * Bundled Adhan sounds. Only a placeholder ships today; real full/short Adhans (Open Item #4) are
 * dropped into res/raw later and added here without touching the playback engine.
 */
enum class AdhanSound(@param:StringRes val labelRes: Int, val rawResName: String) {
    PLACEHOLDER(R.string.sound_placeholder, "adhan_placeholder"),
}

/** Per-prayer alarm preferences. */
data class PrayerAlarmPrefs(
    val enabled: Boolean = true,
    val preWarnMinutes: Int = 0,
)

/** All alarm-related settings (per-prayer enable + pre-warning; global master toggle + sound). */
data class AlarmSettings(
    val masterEnabled: Boolean = true,
    val sound: AdhanSound = AdhanSound.PLACEHOLDER,
    val perPrayer: Map<Prayer, PrayerAlarmPrefs> = Prayer.OBLIGATORY.associateWith { PrayerAlarmPrefs() },
) {
    fun prefs(prayer: Prayer): PrayerAlarmPrefs = perPrayer[prayer] ?: PrayerAlarmPrefs()

    companion object {
        /** Pre-warning options offered in the UI (minutes before Adhan; 0 = exactly at Adhan). */
        val PRE_WARN_OPTIONS = listOf(0, 5, 10, 15)
    }
}
