package de.igbdsandzakkassel.vaktija.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.alarmDataStore by preferencesDataStore(name = "alarm_settings")

/** DataStore-backed alarm settings. */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.alarmDataStore

    fun observe(): Flow<AlarmSettings> = store.data.map { prefs ->
        AlarmSettings(
            masterEnabled = prefs[MASTER_ENABLED] ?: true,
            sound = AdhanSound.fromName(prefs[SOUND]),
            playWhenSilent = prefs[PLAY_WHEN_SILENT] ?: false,
            autoSilenceEnabled = prefs[AUTO_SILENCE] ?: false,
            silenceBeforeMinutes = (prefs[SILENCE_BEFORE] ?: 5)
                .takeIf { it in AlarmSettings.SILENCE_OPTIONS } ?: 5,
            silenceMinutes = (prefs[SILENCE_MINUTES] ?: 10)
                .takeIf { it in AlarmSettings.SILENCE_OPTIONS } ?: 10,
            weeklyReminderEnabled = prefs[WEEKLY_REMINDER] ?: true,
            perPrayer = Prayer.NOTIFIABLE.associateWith { prayer ->
                val isSunrise = prayer == Prayer.SUNRISE
                PrayerAlarmPrefs(
                    // Sunrise defaults OFF: it now sounds the Adhan like the rest, and an
                    // unrequested call to prayer at first light would wake people who never
                    // asked for it. Whoever wants it turns it on.
                    enabled = prefs[enabledKey(prayer)] ?: !isSunrise,
                    // Coerce legacy/invalid values (e.g. a previously-saved 30) to a valid option.
                    // No pre-notice by default any more: the Adhan itself marks the moment, so a
                    // standing 15-minute warning would just be a second alert nobody chose.
                    preWarnMinutes = coercePreWarn(prefs[preWarnKey(prayer)] ?: 0, isSunrise),
                )
            },
        )
    }

    suspend fun setMasterEnabled(enabled: Boolean) {
        store.edit { it[MASTER_ENABLED] = enabled }
    }

    /** The sound/vibration mode played when a prayer time arrives. */
    suspend fun setSound(sound: AdhanSound) {
        store.edit { it[SOUND] = sound.name }
    }

    /** Whether the Adhan plays out loud even when the phone is on silent/vibrate (default off). */
    suspend fun setPlayWhenSilent(enabled: Boolean) {
        store.edit { it[PLAY_WHEN_SILENT] = enabled }
    }

    suspend fun setPrayerEnabled(prayer: Prayer, enabled: Boolean) {
        store.edit { it[enabledKey(prayer)] = enabled }
    }

    suspend fun setPreWarnMinutes(prayer: Prayer, minutes: Int) {
        store.edit { it[preWarnKey(prayer)] = minutes }
    }

    suspend fun setAutoSilence(enabled: Boolean) {
        store.edit { it[AUTO_SILENCE] = enabled }
    }

    suspend fun setSilenceMinutes(minutes: Int) {
        store.edit { it[SILENCE_MINUTES] = minutes }
    }

    suspend fun setSilenceBeforeMinutes(minutes: Int) {
        store.edit { it[SILENCE_BEFORE] = minutes }
    }

    /** Toggle the weekly (Friday) dhikr/hadith reminder. */
    suspend fun setWeeklyReminderEnabled(enabled: Boolean) {
        store.edit { it[WEEKLY_REMINDER] = enabled }
    }

    /** Remembers the phone's DND state before we silenced it, so it can be restored afterwards. */
    suspend fun setSavedInterruptionFilter(filter: Int) {
        store.edit { it[SAVED_FILTER] = filter }
    }

    suspend fun getSavedInterruptionFilter(): Int? = store.data.first()[SAVED_FILTER]

    /**
     * Snaps a stored value onto the options actually offered, so a value written by an older build
     * (or a different prayer's list) can never leave a pill showing something unselectable.
     */
    private fun coercePreWarn(value: Int, sunrise: Boolean = false): Int {
        val options =
            if (sunrise) AlarmSettings.SUNRISE_WARN_OPTIONS else AlarmSettings.PRE_WARN_OPTIONS
        return if (value in options) {
            value
        } else {
            options.minByOrNull { kotlin.math.abs(it - value) } ?: options.first()
        }
    }

    // --- Community announcement (news) notifications ---

    /** Whether the user wants a notification when a new announcement is posted (default on). */
    fun observeNewsNotificationsEnabled(): Flow<Boolean> = store.data.map { it[NEWS_NOTIFS] ?: true }

    suspend fun setNewsNotificationsEnabled(enabled: Boolean) {
        store.edit { it[NEWS_NOTIFS] = enabled }
    }

    /** One-shot read for background checks (worker/receiver). */
    suspend fun getNewsNotificationsEnabled(): Boolean = store.data.first()[NEWS_NOTIFS] ?: true

    /** createdAt (epoch millis) of the newest announcement we've already notified about. */
    suspend fun getLastNotifiedNewsMillis(): Long? = store.data.first()[LAST_NEWS_MILLIS]

    suspend fun setLastNotifiedNewsMillis(millis: Long) {
        store.edit { it[LAST_NEWS_MILLIS] = millis }
    }

    /** `updatedAt` (epoch millis) of the community rules we've already notified about a change for. */
    suspend fun getLastSeenConfigMillis(): Long? = store.data.first()[LAST_CONFIG_MILLIS]

    suspend fun setLastSeenConfigMillis(millis: Long) {
        store.edit { it[LAST_CONFIG_MILLIS] = millis }
    }

    /**
     * Epoch-millis until which WE have an auto-silence (DND) window active, or 0/absent if none.
     * Set when a silence window starts, cleared when it ends. Lets app-open/boot recover a DND that
     * was turned on but whose "restore" alarm was lost (reboot/force-stop) — without ever touching a
     * Do-Not-Disturb the user set themselves (which never writes this key).
     */
    suspend fun getDndActiveUntil(): Long = store.data.first()[DND_ACTIVE_UNTIL] ?: 0L

    suspend fun setDndActiveUntil(millis: Long) {
        store.edit { it[DND_ACTIVE_UNTIL] = millis }
    }

    /**
     * The user's selected app-language tag, persisted so background workers/receivers can localize
     * notifications without relying on AppCompatDelegate (which can read empty on a cold wake-up).
     * Synced from the UI (which reads it reliably with an Activity present).
     */
    suspend fun setLanguageTag(tag: String) {
        store.edit { it[LANGUAGE_TAG] = tag }
    }

    suspend fun getLanguageTag(): String? = store.data.first()[LANGUAGE_TAG]

    /** Whether the first-launch onboarding (language pick + intro) has been completed. */
    fun observeOnboardingComplete(): Flow<Boolean> = store.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingComplete() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    fun observeThemeMode(): Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    /** Persist the month-calendar per-prayer calibration (6 seconds-offsets) for offline reuse. */
    suspend fun saveCalibrationOffsets(offsets: List<Int>) {
        store.edit { it[CALIBRATION] = offsets.joinToString(",") }
    }

    suspend fun getCalibrationOffsets(): List<Int>? =
        store.data.first()[CALIBRATION]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 6 }

    private companion object {
        /** Fifteen minutes before sunrise — inside the 10-30 window members asked for. */

        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val SOUND = stringPreferencesKey("adhan_sound")
        val PLAY_WHEN_SILENT = booleanPreferencesKey("play_when_silent")
        val AUTO_SILENCE = booleanPreferencesKey("auto_silence")
        val SILENCE_MINUTES = intPreferencesKey("silence_minutes")
        val SILENCE_BEFORE = intPreferencesKey("silence_before_minutes")
        val WEEKLY_REMINDER = booleanPreferencesKey("weekly_reminder_enabled")
        val SAVED_FILTER = intPreferencesKey("saved_dnd_filter")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CALIBRATION = stringPreferencesKey("month_calibration")
        val NEWS_NOTIFS = booleanPreferencesKey("news_notifs_enabled")
        val LAST_NEWS_MILLIS = longPreferencesKey("last_notified_news_millis")
        val LAST_CONFIG_MILLIS = longPreferencesKey("last_seen_config_millis")
        val DND_ACTIVE_UNTIL = longPreferencesKey("dnd_active_until")
        val LANGUAGE_TAG = stringPreferencesKey("app_language_tag")
        fun enabledKey(p: Prayer) = booleanPreferencesKey("enabled_${p.name}")
        fun preWarnKey(p: Prayer) = intPreferencesKey("prewarn_${p.name}")
    }
}
