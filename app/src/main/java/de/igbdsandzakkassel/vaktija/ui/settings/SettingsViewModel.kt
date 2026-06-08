package de.igbdsandzakkassel.vaktija.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.AdminController
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.data.settings.AlarmSettings
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.service.alarm.AlarmScheduler
import de.igbdsandzakkassel.vaktija.service.audio.AdhanForegroundService
import de.igbdsandzakkassel.vaktija.service.dnd.DndController
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
    private val dndController: DndController,
    private val adminController: AdminController,
    private val communityRuleProvider: CommunityRuleProvider,
) : ViewModel() {

    val settings: StateFlow<AlarmSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlarmSettings())

    val themeMode: StateFlow<ThemeMode> = settingsRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    /** True while signed in as the admin → reveals the admin editor. */
    val isAdmin: StateFlow<Boolean> = adminController.observeIsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Current community rules (Fajr Iqamah, Jumua, offsets) — what the admin edits. */
    val communityRules: StateFlow<CommunityRules> = communityRuleProvider.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunityRules.DEFAULT)

    fun signIn(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(adminController.signIn(email, password).isSuccess)
        }
    }

    fun signOut() = adminController.signOut()

    fun saveRules(rules: CommunityRules, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { communityRuleProvider.saveRules(rules) }.isSuccess
            onResult(ok)
        }
    }

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

    /**
     * Enables the prayer and sets its alert timing in one step. Used by the per-prayer chips:
     * "0 min" = Adhan exactly on time; "5/10/15 min" = Adhan on time + a reminder that many minutes
     * earlier. ("Off" calls [setPrayerEnabled] with false.)
     */
    fun selectPreWarn(prayer: Prayer, minutes: Int) = applyThenReschedule {
        settingsRepository.setPrayerEnabled(prayer, true)
        settingsRepository.setPreWarnMinutes(prayer, minutes)
    }

    fun setAutoSilence(enabled: Boolean) = applyThenReschedule {
        settingsRepository.setAutoSilence(enabled)
    }

    fun setSilenceMinutes(minutes: Int) = applyThenReschedule {
        settingsRepository.setSilenceMinutes(minutes)
    }

    /** Plays the Adhan immediately so the user can preview it (and verify permissions/audio). */
    fun testAdhan() {
        AdhanForegroundService.start(context, Prayer.DHUHR, AdhanSound.PLACEHOLDER.rawResName)
    }

    fun canScheduleExact(): Boolean = alarmScheduler.canScheduleExact()

    fun hasDndAccess(): Boolean = dndController.hasAccess()

    private fun applyThenReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            alarmScheduler.rescheduleAll()
        }
    }
}
