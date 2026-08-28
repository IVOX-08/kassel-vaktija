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
     * Communities, not towns.
     *
     * A flat list of every town looked tidy with one community, but Berlin runs two separate
     * communities in the same city — as towns they are two rows both reading "Berlin", telling
     * apart only by the small print. People belong to a community, so that is what they pick first.
     *
     * The filter still matches town names, so typing "Korbach" surfaces the community that runs it
     * without the user having to know its name.
     */
    val results: StateFlow<List<Community>> =
        combine(communityRepository.observeSelectable(), _query) { communities, query ->
            val needle = query.fold()
            communities
                .filter { community ->
                    needle.isBlank() ||
                        community.name.fold().contains(needle) ||
                        community.locations.any { it.name.fold().contains(needle) }
                }
                .sortedBy { it.name.fold() }
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

    /**
     * The line under a community's name.
     *
     * Normally its towns, so someone looking for Korbach sees it without opening the community.
     * But IGBD's register holds two communities called exactly "Džemat Stuttgart", both in
     * Stuttgart — as towns they are two identical rows, and picking one would be a coin toss. When
     * a name is shared, the street is what tells them apart, so the street is what is shown.
     */
    fun subtitleFor(community: Community, all: List<Community>): String {
        val shared = all.count { it.name.equals(community.name, ignoreCase = true) } > 1
        val address = community.address
        if (shared && !address.isNullOrBlank()) return address
        return community.locations.joinToString(" · ") { it.name }
    }

    fun select(community: Community, location: CommunityLocation, onDone: () -> Unit) {
        viewModelScope.launch {
            selectionRepository.select(community.id, location.id)
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
