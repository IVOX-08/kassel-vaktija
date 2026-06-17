package de.igbdsandzakkassel.vaktija.ui.ramadan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

data class RamadanUiState(
    val loading: Boolean = false,
    val fasting: Boolean = false,             // currently within the fasting window (Fajr..Maghrib)
    val iftar: LocalTime = LocalTime.MIDNIGHT, // = Maghrib
    val sehurEnd: LocalTime = LocalTime.MIDNIGHT, // = Fajr
    val teravija: LocalTime = LocalTime.MIDNIGHT, // = Isha (Tarawih follows it)
    @StringRes val countdownLabelRes: Int = R.string.ramadan_until_iftar,
    val countdown: String = "00:00:00",
)

/**
 * Drives the Ramadan screen: today's Iftar (Maghrib) and end-of-Suhoor (Fajr) times plus a live
 * countdown to whichever comes next — Iftar while fasting, otherwise the end of Suhoor. Reuses the
 * same prayer-time source as the dashboard.
 */
@HiltViewModel
class RamadanViewModel @Inject constructor(
    private val timesRepository: PrayerTimesRepository,
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    init {
        viewModelScope.launch { timesRepository.refresh() }
    }

    val uiState: StateFlow<RamadanUiState> = combine(
        timesRepository.observeToday(),
        ticker,
    ) { times, _ ->
        if (times == null) RamadanUiState(loading = true) else build(times, LocalDateTime.now())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RamadanUiState(loading = true),
    )

    private fun build(times: DailyTimes, now: LocalDateTime): RamadanUiState {
        val fajr = times.adhan(Prayer.FAJR)
        val maghrib = times.adhan(Prayer.MAGHRIB)
        val isha = times.adhan(Prayer.ISHA)
        val nowT = now.toLocalTime()
        val today = now.toLocalDate()

        val fasting = nowT >= fajr && nowT < maghrib
        val (labelRes, target) = if (fasting) {
            R.string.ramadan_until_iftar to today.atTime(maghrib)
        } else {
            // Before Fajr → end of Suhoor is today; after Maghrib → it's tomorrow.
            val day = if (nowT >= maghrib) today.plusDays(1) else today
            R.string.ramadan_until_sehur to day.atTime(fajr)
        }

        return RamadanUiState(
            loading = false,
            fasting = fasting,
            iftar = maghrib,
            sehurEnd = fajr,
            teravija = isha,
            countdownLabelRes = labelRes,
            countdown = formatCountdown(Duration.between(now, target)),
        )
    }

    private fun formatCountdown(d: Duration): String {
        val total = d.seconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }
}
