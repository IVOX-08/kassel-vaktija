package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.community.CommunitySelectionRepository
import de.igbdsandzakkassel.vaktija.ui.theme.PageBackgroundLight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the TV shows: the board, or — on a freshly mounted TV — the community picker first.
 *
 * The phone asks this during onboarding, which the TV skips entirely (there is no keyboard to type
 * a language into and no one to swipe through an intro). Without asking here, every board in every
 * mosque would come up showing Kassel.
 */
@Composable
fun TvRoot(modifier: Modifier = Modifier) {
    val viewModel: TvRootViewModel = hiltViewModel()
    val showPicker by viewModel.showPicker.collectAsStateWithLifecycle()

    when (showPicker) {
        // Undecided for the first frame or two while the stored choice is read. Painting the board
        // meanwhile would flash Kassel's times onto a wall in another town.
        null -> Box(modifier.fillMaxSize().background(PageBackgroundLight))
        true -> TvCommunityPicker(onDone = viewModel::picked, modifier = modifier)
        false -> TvDashboardScreen(
            onChangeCommunity = viewModel::openPicker,
            modifier = modifier,
        )
    }
}

@HiltViewModel
class TvRootViewModel @Inject constructor(
    private val selectionRepository: CommunitySelectionRepository,
) : ViewModel() {

    private val _showPicker = MutableStateFlow<Boolean?>(null)
    val showPicker: StateFlow<Boolean?> = _showPicker.asStateFlow()

    init {
        viewModelScope.launch {
            // An install that already follows a community keeps it: a board that has hung in the
            // prayer hall for a year must not start asking questions after an update.
            _showPicker.value = !selectionRepository.hasChosen()
        }
    }

    /** Reopens the picker — the board's way back when the wrong community was chosen. */
    fun openPicker() { _showPicker.value = true }

    fun picked() { _showPicker.value = false }
}
