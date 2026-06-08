package de.igbdsandzakkassel.vaktija.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.hadith.HadithItem
import de.igbdsandzakkassel.vaktija.data.hadith.HadithRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HadithViewModel @Inject constructor(
    private val repository: HadithRepository,
) : ViewModel() {

    /** Hadith list; null while loading. */
    private val _items = MutableStateFlow<List<HadithItem>?>(null)
    val items: StateFlow<List<HadithItem>?> = _items.asStateFlow()

    private var loaded = false

    fun load(collection: String, lang: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _items.value = withContext(Dispatchers.IO) { repository.load(collection, lang) }
        }
    }
}
