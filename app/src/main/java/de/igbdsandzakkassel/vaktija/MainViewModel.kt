package de.igbdsandzakkassel.vaktija

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.CommunityStatus
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    communityRepository: CommunityRepository,
) : ViewModel() {
    /** Status of the followed community — decides whether the app renders at all. */
    val communityStatus: StateFlow<CommunityStatus> = communityRepository.observeSelection()
        .map { it?.community?.status ?: CommunityStatus.ACTIVE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunityStatus.ACTIVE)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** null while loading (avoids flashing the wrong screen), then true/false. */
    val onboardingComplete: StateFlow<Boolean?> = settingsRepository.observeOnboardingComplete()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Mirror the app-language tag into settings so background code can localize notifications.
        //
        // The `?.let` is the whole point of this line. It used to write
        // `LocaleController.current().tag`, which falls back to Bosnian whenever
        // AppCompatDelegate reads empty — and it reads empty for a moment right after a
        // locale-change Activity recreate, which is precisely when this runs. So a user who had
        // just chosen German got "bs" written over his real choice, and the prayer tracker then
        // asked him "Jesi li klanjao Akšam?" for good.
        //
        // A guess must never be stored as if it were an answer. When we do not know, we leave
        // what is there.
        viewModelScope.launch {
            LocaleController.resolvedTag(context)?.let { settingsRepository.setLanguageTag(it) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete() }
    }
}
