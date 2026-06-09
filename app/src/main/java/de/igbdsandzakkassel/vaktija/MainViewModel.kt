package de.igbdsandzakkassel.vaktija

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settingsRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** null while loading (avoids flashing the wrong screen), then true/false. */
    val onboardingComplete: StateFlow<Boolean?> = settingsRepository.observeOnboardingComplete()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Persist the current app-language tag so background workers can localize notifications
        // (read reliably here with an Activity present, unlike on a cold background wake-up).
        viewModelScope.launch { settingsRepository.setLanguageTag(LocaleController.current().tag) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete() }
    }
}
