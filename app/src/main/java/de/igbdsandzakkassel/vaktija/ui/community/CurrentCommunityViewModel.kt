package de.igbdsandzakkassel.vaktija.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.CommunitySelection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The mosque this device currently follows — for the Settings row and the inactive notice. */
@HiltViewModel
class CurrentCommunityViewModel @Inject constructor(
    communityRepository: CommunityRepository,
) : ViewModel() {

    val selection: StateFlow<CommunitySelection?> = communityRepository.observeSelection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
