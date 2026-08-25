package de.igbdsandzakkassel.vaktija.ui.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.community.CommunitySelectionRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val selectionRepository: CommunitySelectionRepository,
) : ViewModel() {

    /**
     * Whether this install already follows a community. False only on a genuinely fresh install —
     * an install updated from a single-community build counts as having chosen, so those users are
     * never sent to the picker.
     */
    suspend fun hasChosenCommunity(): Boolean = selectionRepository.hasChosen()
}
