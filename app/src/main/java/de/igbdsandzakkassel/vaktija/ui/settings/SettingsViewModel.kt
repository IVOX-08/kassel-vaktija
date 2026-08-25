package de.igbdsandzakkassel.vaktija.ui.settings

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.AdminRole
import de.igbdsandzakkassel.vaktija.data.model.CommunitySelection
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
import kotlinx.coroutines.flow.combine
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
    private val communityRepository: CommunityRepository,
    private val communityRuleProvider: CommunityRuleProvider,
) : ViewModel() {

    val settings: StateFlow<AlarmSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlarmSettings())

    val themeMode: StateFlow<ThemeMode> = settingsRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    /** Whether the user wants a (specially-toned) notification for new community announcements. */
    val newsNotificationsEnabled: StateFlow<Boolean> = settingsRepository.observeNewsNotificationsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * Whether the signed-in account may administer the community currently being viewed.
     *
     * Both halves matter: a community admin looking at another community must see no controls, and
     * the head admin must see them everywhere. The Firestore rules enforce the same condition, so
     * this only decides what is shown — it is not the thing standing between an account and the
     * data.
     */
    val isAdmin: StateFlow<Boolean> = combine(
        adminController.observeRole(),
        communityRepository.observeSelection(),
    ) { role, selection -> role.canAdminister(selection?.community?.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Current community rules (Fajr Iqamah, Jumua, offsets) — what the admin edits. */
    val communityRules: StateFlow<CommunityRules> = communityRuleProvider.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunityRules.DEFAULT)

    /** The community being viewed — named in the admin header so the scope is never in doubt. */
    val selection: StateFlow<CommunitySelection?> = communityRepository.observeSelection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The signed-in account's role — the Settings header names it, so nobody guesses. */
    val adminRole: StateFlow<AdminRole> = adminController.observeRole()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminRole.None)

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

    fun setSound(sound: AdhanSound) = applyThenReschedule {
        settingsRepository.setSound(sound)
    }

    /** Whether the Adhan should play out loud even when the phone is on silent/vibrate. */
    fun setPlayWhenSilent(enabled: Boolean) = applyThenReschedule {
        settingsRepository.setPlayWhenSilent(enabled)
    }

    fun setNewsNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNewsNotificationsEnabled(enabled) }
    }

    fun setPrayerEnabled(prayer: Prayer, enabled: Boolean) = applyThenReschedule {
        settingsRepository.setPrayerEnabled(prayer, enabled)
    }

    fun setPreWarn(prayer: Prayer, minutes: Int) = applyThenReschedule {
        settingsRepository.setPreWarnMinutes(prayer, minutes)
    }

    fun setAutoSilence(enabled: Boolean) = applyThenReschedule {
        settingsRepository.setAutoSilence(enabled)
    }

    fun setWeeklyReminderEnabled(enabled: Boolean) = applyThenReschedule {
        settingsRepository.setWeeklyReminderEnabled(enabled)
    }

    fun setSilenceMinutes(minutes: Int) = applyThenReschedule {
        settingsRepository.setSilenceMinutes(minutes)
    }

    fun setSilenceBeforeMinutes(minutes: Int) = applyThenReschedule {
        settingsRepository.setSilenceBeforeMinutes(minutes)
    }

    /** Previews the currently-selected prayer sound so the user can verify it (and permissions). */
    fun testAdhan() {
        AdhanForegroundService.start(context, Prayer.DHUHR, settings.value.sound.name)
    }

    fun canScheduleExact(): Boolean = alarmScheduler.canScheduleExact()

    fun hasDndAccess(): Boolean = dndController.hasAccess()

    /** True if the app is already exempt from battery optimization (then the button is hidden). */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun applyThenReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            alarmScheduler.rescheduleAll()
        }
    }
}
