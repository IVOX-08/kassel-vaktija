package de.igbdsandzakkassel.vaktija.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.community.CommunitySelectionRepository
import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.data.model.CommunityLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

/** One row in the picker: a town, plus the community that runs it. */
data class MosqueEntry(
    val community: Community,
    val location: CommunityLocation,
)

@HiltViewModel
class CommunityPickerViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val selectionRepository: CommunitySelectionRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Every town of every listed community, flattened — people look for their MOSQUE, not for an
     * administrative body, and flattening is also what makes the two awkward cases read naturally:
     * Kassel's three towns appear as three rows under one name, and Berlin's two communities appear
     * as two rows with the same town but different names.
     */
    val results: StateFlow<List<MosqueEntry>> =
        combine(communityRepository.observeSelectable(), _query) { communities, query ->
            val needle = query.fold()
            communities
                .flatMap { community -> community.locations.map { MosqueEntry(community, it) } }
                .filter { entry ->
                    needle.isBlank() ||
                        entry.location.name.fold().contains(needle) ||
                        entry.community.name.fold().contains(needle)
                }
                .sortedWith(compareBy({ it.location.name.fold() }, { it.community.name.fold() }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Whether the catalogue has answered at all. Without this the screen cannot tell "still
     * loading" from "loaded, and there is genuinely nothing to show" — and an empty list rendered
     * as a spinner leaves the user staring at it forever, which is what happens the moment every
     * community in range is switched off or the catalogue read fails.
     */
    val loaded: StateFlow<Boolean> = communityRepository.observeSelectable()
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onQueryChange(value: String) { _query.value = value }

    fun select(entry: MosqueEntry, onDone: () -> Unit) {
        viewModelScope.launch {
            selectionRepository.select(entry.community.id, entry.location.id)
            onDone()
        }
    }

    /**
     * Lowercase and strip accents so searching works the way people actually type: "munden" finds
     * "Münden", "sandzak" finds "Sandžak". Without this, the diacritics in Bosnian names make the
     * search feel broken to anyone on a German keyboard.
     */
    private fun String.fold(): String =
        Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace("đ", "d")
            .replace("ß", "ss")

    private companion object {
        val DIACRITICS = Regex("""\p{Mn}+""")
    }
}
