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
            while (true) {
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
        // Both lists are built from the same Arabic source order, so the index maps to the same
        // hadith (in bs and de). Pair them up.
        val bs = COLLECTIONS.flatMap { repository.load(it, "bs") }
        if (bs.isEmpty()) return null
        val de = COLLECTIONS.flatMap { repository.load(it, "de") }
        val all = bs.indices.map { i ->
            val b = clean(bs[i].translation.ifBlank { bs[i].arabic })
            val dRaw = de.getOrNull(i)?.translation?.ifBlank { null }
            val d = if (dRaw != null) clean(dRaw) else b
            DailyHadith(bs = b, de = d)
        }.filter { it.bs.isNotEmpty() }
        if (all.isEmpty()) return null
        // The wall board has limited room: prefer SHORT hadiths (in BOTH languages) so they show in
        // full instead of being cut off with "…". Fall back to the full set only if none are short.
        val pool = all.filter { it.bs.length <= MAX_CHARS && it.de.length <= MAX_CHARS }.ifEmpty { all }
        val index = LocalDate.now().toEpochDay().mod(pool.size)
        return pool[index]
    }

    /**
     * Strip a trailing source/narrator note like "(überliefert von …; al-Bukhari)" or "[Buhari]" so
     * the board shows only the hadith itself. (All our hadiths are from the authentic collections —
     * Bukhari/Muslim/Tirmidhi … via the 40 Nawawi + Riyad as-Salihin assets.)
     */
    /** Trim a hadith down to just the saying for the wall board: drop the leading narrator chain
     *  ("Od Ebu-Hurejre, r.a., … rekao:" / "X berichtete … sagte:") and any trailing source note. */
    private fun clean(text: String): String = stripAttribution(stripIsnad(text))

    private fun stripIsnad(text: String): String {
        val t = text.trim()
        // If it opens with a narrator chain that ends in a colon early on, drop up to that colon.
        val m = Regex(
            "^.{0,170}?(rekao|kazao|rekla|veli|prenosi|kaže|berichte|sagte|sprach|überliefert)[^:]{0,45}:\\s+",
            RegexOption.IGNORE_CASE,
        ).find(t)
        return (if (m != null) t.removeRange(m.range) else t).trim()
    }

    private fun stripAttribution(text: String): String {
        var t = text.trim()
        val tail = Regex("\\s*[(\\[][^)\\]]*[)\\]]\\s*$")
        while (tail.containsMatchIn(t)) t = t.replace(tail, "").trim()
        return t
    }

    private companion object {
        val COLLECTIONS = listOf("riyadussalihin", "nawawi40")
        // Short enough for ~2 small lines on the TV band so it never pushes the board off-screen.
        const val MAX_CHARS = 190
        // The hadith band swaps language every 30s (slower than the 5s prayer board) with a hint.
        const val SWITCH_SECONDS = 30
    }
}
