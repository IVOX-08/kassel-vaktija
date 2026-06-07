package de.igbdsandzakkassel.vaktija.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
            sound = AdhanSound.PLACEHOLDER,
            autoSilenceEnabled = prefs[AUTO_SILENCE] ?: false,
            silenceMinutes = (prefs[SILENCE_MINUTES] ?: 15)
                .takeIf { it in AlarmSettings.SILENCE_OPTIONS } ?: 15,
            perPrayer = Prayer.OBLIGATORY.associateWith { prayer ->
                PrayerAlarmPrefs(
                    enabled = prefs[enabledKey(prayer)] ?: true,
                    // Coerce legacy/invalid values (e.g. a previously-saved 30) to a valid option.
                    preWarnMinutes = coercePreWarn(prefs[preWarnKey(prayer)] ?: 0),
                )
            },
        )
    }

    suspend fun setMasterEnabled(enabled: Boolean) {
        store.edit { it[MASTER_ENABLED] = enabled }
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

    /** Remembers the phone's DND state before we silenced it, so it can be restored afterwards. */
    suspend fun setSavedInterruptionFilter(filter: Int) {
        store.edit { it[SAVED_FILTER] = filter }
    }

    suspend fun getSavedInterruptionFilter(): Int? = store.data.first()[SAVED_FILTER]

    private fun coercePreWarn(value: Int): Int =
        if (value in AlarmSettings.PRE_WARN_OPTIONS) {
            value
        } else {
            AlarmSettings.PRE_WARN_OPTIONS.minByOrNull { kotlin.math.abs(it - value) } ?: 0
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
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val AUTO_SILENCE = booleanPreferencesKey("auto_silence")
        val SILENCE_MINUTES = intPreferencesKey("silence_minutes")
        val SAVED_FILTER = intPreferencesKey("saved_dnd_filter")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CALIBRATION = stringPreferencesKey("month_calibration")
        fun enabledKey(p: Prayer) = booleanPreferencesKey("enabled_${p.name}")
        fun preWarnKey(p: Prayer) = intPreferencesKey("prewarn_${p.name}")
    }
}
