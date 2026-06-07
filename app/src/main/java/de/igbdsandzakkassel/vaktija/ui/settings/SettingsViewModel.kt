package de.igbdsandzakkassel.vaktija.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.data.settings.AlarmSettings
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.service.alarm.AlarmScheduler
import de.igbdsandzakkassel.vaktija.service.audio.AdhanForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    val settings: StateFlow<AlarmSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlarmSettings())

    val themeMode: StateFlow<ThemeMode> = settingsRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setMasterEnabled(enabled: Boolean) = applyThenReschedule {
        settingsRepository.setMasterEnabled(enabled)
    }

    fun setPrayerEnabled(prayer: Prayer, enabled: Boolean) = applyThenReschedule {
        settingsRepository.setPrayerEnabled(prayer, enabled)
    }

    fun setPreWarn(prayer: Prayer, minutes: Int) = applyThenReschedule {
        settingsRepository.setPreWarnMinutes(prayer, minutes)
    }

    /** Plays the Adhan immediately so the user can preview it (and verify permissions/audio). */
    fun testAdhan() {
        AdhanForegroundService.start(context, Prayer.DHUHR, AdhanSound.PLACEHOLDER.rawResName)
    }

    fun canScheduleExact(): Boolean = alarmScheduler.canScheduleExact()

    private fun applyThenReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            alarmScheduler.rescheduleAll()
        }
    }
}
