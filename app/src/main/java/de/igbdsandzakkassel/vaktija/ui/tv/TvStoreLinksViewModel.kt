package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.store.StoreLinks
import de.igbdsandzakkassel.vaktija.data.store.StoreLinksRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The two download links for the board's QR codes, kept live so a board that has been on the wall
 * for months picks up the iPhone link the moment it is entered — without anyone touching the TV.
 */
@HiltViewModel
class TvStoreLinksViewModel @Inject constructor(
    repository: StoreLinksRepository,
) : ViewModel() {

    val links: StateFlow<StoreLinks> = repository.observe().stateIn(
        scope = viewModelScope,
        // Eagerly: the board is the only screen on this device and never goes to the background,
        // so there is nothing to save by tearing the listener down.
        started = SharingStarted.Eagerly,
        initialValue = StoreLinks(android = StoreLinksRepository.PLAY_URL, ios = ""),
    )
}
