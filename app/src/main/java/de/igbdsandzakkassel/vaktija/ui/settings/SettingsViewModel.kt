package de.igbdsandzakkassel.vaktija.ui.settings

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.community.CommunityCatalog
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.CommunityStatus
import kotlinx.coroutines.flow.map
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
     * Whether to show the admin section at all — true for any signed-in admin, so a head admin
     * still gets his sign-out and (in debug) the role switch. What he may DO inside it is a
     * separate question, answered by [canEditTimes].
     */
    val isAdmin: StateFlow<Boolean> = combine(
        adminController.observeRole(),
        communityRepository.observeSelection(),
    ) { role, _ -> role.isAdmin }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Whether this account may edit THIS community's prayer times.
     *
     * Deliberately false for the head admin: Iqamah and Jumu'ah are the community's own religious
     * decision, and the operator of the programme has no business changing them. The Firestore
     * rules enforce the same — this only decides what is shown.
     */
    val canEditTimes: StateFlow<Boolean> = combine(
        adminController.observeRole(),
        communityRepository.observeSelection(),
    ) { role, selection -> role.canEditTimes(selection?.community?.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * The name of the community this account administers — NOT the one currently being viewed.
     *
     * The header used to print the selected community's name next to "Administrator", so switching
     * to Rosenheim made a Kassel admin read "Administrator: Rosenheim". That is precisely backwards
     * from what the label is for.
     */
    val adminCommunityName: StateFlow<String?> = combine(
        adminController.observeRole(),
        communityRepository.observeAll(),
    ) { role, all ->
        (role as? AdminRole.Community)?.let { r ->
            all.firstOrNull { it.id == r.communityId }?.name ?: r.communityId
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True for the head admin — reveals the community management list. */
    val canManageCommunities: StateFlow<Boolean> = adminController.observeRole()
        .map { it.canManageCommunities }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Every community, switched-off ones included — the head admin has to see those especially. */
    val allCommunities: StateFlow<List<Community>> = communityRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCommunityStatus(communityId: String, status: CommunityStatus) =
        communityRepository.setStatus(communityId, status)

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

    /** Debug builds only: swap the stand-in role so both sides can be tried without Firestore. */
    fun setDebugRole(head: Boolean) {
        adminController.debugRole =
            if (head) AdminRole.Head
            else AdminRole.Community(CommunityCatalog.KASSEL_ID)
    }

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
