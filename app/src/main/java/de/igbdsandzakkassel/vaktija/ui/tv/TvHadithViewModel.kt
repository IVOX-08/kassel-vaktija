package de.igbdsandzakkassel.vaktija.ui.tv

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
import java.time.LocalDate
import javax.inject.Inject

/**
 * Picks the "Hadith of the day" for the TV board. Rotates through the curated Riyad as-Salihin plus
 * the 40 Nawawi hadith in the chosen language, choosing deterministically by the calendar day so the
 * same hadith shows all day and is identical on every device, then changes at midnight.
 */
@HiltViewModel
class TvHadithViewModel @Inject constructor(
    private val repository: HadithRepository,
) : ViewModel() {

    private val _daily = MutableStateFlow<HadithItem?>(null)
    val daily: StateFlow<HadithItem?> = _daily.asStateFlow()

    private var loadedLang: String? = null

    fun load(lang: String) {
        if (lang == loadedLang) return
        loadedLang = lang
        viewModelScope.launch {
            _daily.value = withContext(Dispatchers.IO) {
                val all = COLLECTIONS
                    .flatMap { repository.load(it, lang) }
                    .filter { it.translation.isNotBlank() || it.arabic.isNotBlank() }
                if (all.isEmpty()) return@withContext null
                val index = LocalDate.now().toEpochDay().mod(all.size)
                all[index]
            }
        }
    }

    private companion object {
        // Concise, famous hadith first (Riyad as-Salihin), then the 40 Nawawi, for ~2-month variety.
        val COLLECTIONS = listOf("riyadussalihin", "nawawi40")
    }
}
