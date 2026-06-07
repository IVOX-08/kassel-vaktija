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
            perPrayer = Prayer.OBLIGATORY.associateWith { prayer ->
                PrayerAlarmPrefs(
                    enabled = prefs[enabledKey(prayer)] ?: true,
                    preWarnMinutes = prefs[preWarnKey(prayer)] ?: 0,
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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CALIBRATION = stringPreferencesKey("month_calibration")
        fun enabledKey(p: Prayer) = booleanPreferencesKey("enabled_${p.name}")
        fun preWarnKey(p: Prayer) = intPreferencesKey("prewarn_${p.name}")
    }
}
