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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class RamadanUiState(
    val loading: Boolean = false,
    val fasting: Boolean = false,                 // currently within the fasting window (Fajr..Maghrib)
    val iftar: LocalTime = LocalTime.MIDNIGHT,    // = Maghrib
    val sehurEnd: LocalTime = LocalTime.MIDNIGHT, // = Fajr (Imsak / end of Suhoor)
    val teravija: LocalTime = LocalTime.MIDNIGHT, // = Isha (Tarawih follows it)
    @StringRes val countdownLabelRes: Int = R.string.ramadan_until_iftar,
    val countdown: String = "00:00:00",
    val progress: Float = 0f,                     // 0..1 filled toward the next event (Iftar / Suhoor-end)
    val isRamadan: Boolean = false,
    val ramadanDay: Int = 0,                      // 1..30 during Ramadan, else 0
    val ramadanLength: Int = 30,                  // 29 or 30 (Hijri length of Ramadan)
    val hijriYear: Int = 0,
    val daysUntilRamadan: Int = 0,                // shown when it is NOT Ramadan
)

/**
 * Drives the Ramadan screen: today's Iftar (Maghrib) and end-of-Suhoor (Fajr / Imsak) times, a live
 * countdown to whichever comes next (Iftar while fasting, otherwise the end of Suhoor) with a filling
 * progress fraction, plus the Hijri Ramadan day (or a days-until-Ramadan count when it isn't Ramadan).
 * Reuses the same prayer-time source as the dashboard.
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
        // The window we're currently counting through, so the ring fills smoothly toward the next event.
        val (labelRes, windowStart, target) = when {
            fasting -> Triple(R.string.ramadan_until_iftar, today.atTime(fajr), today.atTime(maghrib))
            // After Iftar tonight → the next event is tomorrow's end of Suhoor.
            nowT >= maghrib -> Triple(R.string.ramadan_until_sehur, today.atTime(maghrib), today.plusDays(1).atTime(fajr))
            // After midnight, before Fajr → end of Suhoor is later today.
            else -> Triple(R.string.ramadan_until_sehur, today.minusDays(1).atTime(maghrib), today.atTime(fajr))
        }
        val totalSecs = Duration.between(windowStart, target).seconds.coerceAtLeast(1)
        val elapsedSecs = Duration.between(windowStart, now).seconds.coerceIn(0, totalSecs)
        val progress = elapsedSecs.toFloat() / totalSecs

        // Hijri (Umm al-Qura) — the same calendar the dashboard's Hijri date uses.
        val h = HijrahDate.from(today)
        val hMonth = h.get(ChronoField.MONTH_OF_YEAR)
        val hYear = h.get(ChronoField.YEAR)
        val isRamadan = hMonth == 9 // Ramadan is the 9th Hijri month.
        val ramadanThisYear = LocalDate.from(HijrahDate.of(hYear, 9, 1))
        val nextRamadan =
            if (ramadanThisYear.isAfter(today)) ramadanThisYear
            else LocalDate.from(HijrahDate.of(hYear + 1, 9, 1))

        return RamadanUiState(
            loading = false,
            fasting = fasting,
            iftar = maghrib,
            sehurEnd = fajr,
            teravija = isha,
            countdownLabelRes = labelRes,
            // Zone-aware so the countdown doesn't jump by 1 h across a DST changeover night.
            countdown = formatCountdown(
                Duration.between(
                    now.atZone(java.time.ZoneId.systemDefault()),
                    target.atZone(java.time.ZoneId.systemDefault()),
                ),
            ),
            progress = progress,
            isRamadan = isRamadan,
            ramadanDay = if (isRamadan) h.get(ChronoField.DAY_OF_MONTH) else 0,
            ramadanLength = HijrahDate.of(hYear, 9, 1).lengthOfMonth(),
            hijriYear = hYear,
            daysUntilRamadan = ChronoUnit.DAYS.between(today, nextRamadan).toInt(),
        )
    }

    private fun formatCountdown(d: Duration): String {
        val total = d.seconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }
}
