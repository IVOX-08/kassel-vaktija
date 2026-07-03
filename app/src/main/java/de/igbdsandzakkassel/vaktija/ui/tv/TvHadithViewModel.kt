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
 * "Hadith of the day" for the TV board. One hadith is chosen per calendar day from a hand-curated
 * list of SHORT, in-themselves-COMPLETE hadiths (assets/hadith/board.json) — picked so the wall
 * display shows a full, readable saying instead of a cut-off fragment. The choice is deterministic by
 * epoch-day (the same on every device, changes at midnight). Each hadith is stored in both Bosnian
 * and German; the shown language then alternates every [SWITCH_SECONDS] seconds so the board cycles
 * through the community's two main languages.
 */
@HiltViewModel
class TvHadithViewModel @Inject constructor(
    private val repository: HadithRepository,
) : ViewModel() {

    data class DailyHadith(val bs: String, val de: String)

    private val _daily = MutableStateFlow<DailyHadith?>(null)
    val daily: StateFlow<DailyHadith?> = _daily.asStateFlow()

    /** Which language to show right now; flips every [SWITCH_SECONDS] seconds. */
    private val _german = MutableStateFlow(false)
    val german: StateFlow<Boolean> = _german.asStateFlow()

    /** Seconds until the next language switch ([SWITCH_SECONDS] → 1) — for the small on-board hint. */
    private val _secondsLeft = MutableStateFlow(SWITCH_SECONDS)
    val secondsLeft: StateFlow<Int> = _secondsLeft.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _daily.value = withContext(Dispatchers.IO) { loadDaily() }
        }
        viewModelScope.launch {
            // The wall board runs 24/7 without ever recreating this ViewModel, so the "daily"
            // hadith must be re-picked when the calendar day changes — not just once at startup.
            var loadedFor = LocalDate.now()
            while (true) {
                val today = LocalDate.now()
                if (today != loadedFor) {
                    loadedFor = today
                    _daily.value = withContext(Dispatchers.IO) { loadDaily() }
                }
                val sec = LocalTime.now().toSecondOfDay()
                // 30-second blocks: even block = Bosnian, odd = German. Deterministic and resets
                // cleanly at midnight (86400 is a whole number of blocks).
                _german.value = (sec / SWITCH_SECONDS) % 2 == 1
                _secondsLeft.value = SWITCH_SECONDS - (sec % SWITCH_SECONDS)
                delay(1000)
            }
        }
    }

    private fun loadDaily(): DailyHadith? {
        val board = repository.loadBoard()
        if (board.isEmpty()) return null
        // Same hadith for the whole day on every device; advances at midnight.
        val index = LocalDate.now().toEpochDay().mod(board.size)
        val h = board[index]
        return DailyHadith(bs = h.bs, de = h.de)
    }

    private companion object {
        // The hadith band swaps language every 30s (slower than the 5s prayer board) with a hint.
        const val SWITCH_SECONDS = 30
    }
}
