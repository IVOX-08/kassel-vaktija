package de.igbdsandzakkassel.vaktija.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.quran.Ayah
import de.igbdsandzakkassel.vaktija.data.quran.QuranRepository
import de.igbdsandzakkassel.vaktija.data.quran.SurahMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val repository: QuranRepository,
) : ViewModel() {

    /** All 114 surahs (chapter list); null while loading. */
    val surahs: StateFlow<List<SurahMeta>?> = flow { emit(repository.surahs()) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _meta = MutableStateFlow<SurahMeta?>(null)
    val meta: StateFlow<SurahMeta?> = _meta.asStateFlow()

    private val _ayahs = MutableStateFlow<List<Ayah>?>(null)
    val ayahs: StateFlow<List<Ayah>?> = _ayahs.asStateFlow()

    private var loadedSurah = -1

    fun loadSurah(id: Int) {
        if (loadedSurah == id) return
        loadedSurah = id
        viewModelScope.launch {
            val (meta, ayahs) = withContext(Dispatchers.IO) {
                repository.surahs().firstOrNull { it.id == id } to repository.ayahs(id)
            }
            _meta.value = meta
            _ayahs.value = ayahs
        }
    }
}
