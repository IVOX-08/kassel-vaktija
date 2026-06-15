package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.hadith.HadithRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * "Hadith of the day" for the TV board. One hadith is chosen per calendar day (deterministic by
 * epoch-day, so it's the same on every device and changes at midnight), loaded in BOTH Bosnian and
 * German. The displayed language then alternates every minute (even minute = Bosnian, odd = German)
 * so the wall display shows the community's two main languages in turn.
 */
@HiltViewModel
class TvHadithViewModel @Inject constructor(
    private val repository: HadithRepository,
) : ViewModel() {

    data class DailyHadith(val bs: String, val de: String)

    private val _daily = MutableStateFlow<DailyHadith?>(null)
    val daily: StateFlow<DailyHadith?> = _daily.asStateFlow()

    /** Which language to show right now; flips on every minute boundary. */
    private val _german = MutableStateFlow(false)
    val german: StateFlow<Boolean> = _german.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _daily.value = withContext(Dispatchers.IO) { loadDaily() }
        }
        viewModelScope.launch {
            while (true) {
                val now = LocalTime.now()
                _german.value = now.minute % 2 == 1
                // Re-evaluate exactly on the next minute boundary.
                delay((60 - now.second).coerceAtLeast(1) * 1000L)
            }
        }
    }

    private fun loadDaily(): DailyHadith? {
        // Both lists are built from the same Arabic source order, so the index maps to the same
        // hadith in either language.
        val bs = COLLECTIONS.flatMap { repository.load(it, "bs") }
        if (bs.isEmpty()) return null
        val de = COLLECTIONS.flatMap { repository.load(it, "de") }
        val index = LocalDate.now().toEpochDay().mod(bs.size)
        val bsText = bs[index].translation.ifBlank { bs[index].arabic }
        val deText = de.getOrNull(index)?.translation?.ifBlank { null } ?: bsText
        return DailyHadith(bs = bsText, de = deText)
    }

    private companion object {
        val COLLECTIONS = listOf("riyadussalihin", "nawawi40")
    }
}
