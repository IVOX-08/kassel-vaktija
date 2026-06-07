package de.igbdsandzakkassel.vaktija.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.domain.PrayerScheduleCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val timesRepository: PrayerTimesRepository,
    ruleProvider: CommunityRuleProvider,
) : ViewModel() {

    // Emits once per second to drive the live clock + countdown.
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    init {
        // Pull the latest times on open (idempotent; WorkManager also refreshes in the background).
        viewModelScope.launch { timesRepository.refresh() }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        timesRepository.observeToday(),
        timesRepository.observeFreshness(),
        ruleProvider.observeRules(),
        ticker,
    ) { times, fresh, rules, _ ->
        if (times == null) DashboardUiState(loading = true)
        else buildState(times, rules, fresh, LocalDateTime.now())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(loading = true),
    )

    private fun buildState(
        times: DailyTimes,
        rules: CommunityRules,
        fresh: Boolean,
        now: LocalDateTime,
    ): DashboardUiState {
        val locale = Locale.getDefault()
        val schedule = PrayerScheduleCalculator.compute(times, now)

        val rows = Prayer.entries.map { prayer ->
            val adhan = times.adhan(prayer)
            PrayerRowUi(
                prayer = prayer,
                adhan = adhan,
                iqamah = rules.iqamah(prayer, adhan),
                // Highlight the NEXT upcoming prayer (matches the countdown above).
                isHighlighted = prayer == schedule.nextPrayer,
            )
        }

        return DashboardUiState(
            loading = false,
            clock = now.format(CLOCK),
            gregorianDate = now.toLocalDate().format(dateFormatter(locale)),
            hijriDate = HijrahDate.from(now.toLocalDate()).format(hijriFormatter(locale)),
            rows = rows,
            nextPrayer = schedule.nextPrayer,
            countdown = formatCountdown(Duration.between(now, schedule.nextAdhan)),
            jumua = rules.jumua,
            isStale = !fresh,
        )
    }

    private fun formatCountdown(d: Duration): String {
        val total = d.seconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        fun dateFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", locale)
        fun hijriFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern("d. MMMM yyyy G", locale)
    }
}
