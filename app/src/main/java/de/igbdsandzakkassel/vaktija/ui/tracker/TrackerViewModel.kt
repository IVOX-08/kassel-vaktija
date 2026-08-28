package de.igbdsandzakkassel.vaktija.ui.tracker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.tracker.DayLog
import de.igbdsandzakkassel.vaktija.data.tracker.PrayerAnswer
import de.igbdsandzakkassel.vaktija.data.tracker.PrayerLogRepository
import de.igbdsandzakkassel.vaktija.domain.PrayerWindows
import de.igbdsandzakkassel.vaktija.service.tracker.TrackerNotifier
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/** Where one prayer stands today. */
enum class RowState {
    /** Its time has not come yet. */
    UPCOMING,

    /** Answerable right now. */
    OPEN,

    /** Answered: prayed. */
    PRAYED,

    /** Answered: not prayed. */
    NOT_PRAYED,

    /** The window closed with no answer — counts the same as not prayed, and cannot be changed. */
    MISSED,
}

data class TrackerRow(
    val prayer: Prayer,
    val opensAt: LocalTime,
    val closesAt: LocalTime,
    val state: RowState,
)

data class TrackerUiState(
    val loading: Boolean = true,
    val streak: Int = 0,
    val rows: List<TrackerRow> = emptyList(),
) {
    val prayedToday: Int get() = rows.count { it.state == RowState.PRAYED }
    val openRow: TrackerRow? get() = rows.firstOrNull { it.state == RowState.OPEN }
}

/** Days in a row that earn the mosque's reward. */
const val REWARD_DAYS = 30

@HiltViewModel
class TrackerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: PrayerLogRepository,
    private val timesRepository: PrayerTimesRepository,
    ruleProvider: CommunityRuleProvider,
) : ViewModel() {

    // A minute is enough: windows open and close on minute boundaries, and a screen that repaints
    // every second for a list of five rows would keep the display awake for nothing.
    private val minuteTicker = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }

    val uiState: StateFlow<TrackerUiState> = combine(
        timesRepository.observeToday(),
        ruleProvider.observeRules(),
        logRepository.observeDay(LocalDate.now()),
        logRepository.observeStreak(LocalDate.now()),
        minuteTicker,
    ) { times, rules, log, streak, now ->
        if (times == null) return@combine TrackerUiState(loading = true)
        val today = LocalDate.now()
        TrackerUiState(
            loading = false,
            streak = streak,
            rows = Prayer.OBLIGATORY.map { prayer ->
                val window = PrayerWindows.windowFor(prayer, today, times, rules)
                TrackerRow(
                    prayer = prayer,
                    opensAt = window.opensAt.toLocalTime(),
                    closesAt = window.closesAt.toLocalTime(),
                    state = stateOf(log, prayer, window, now),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState())

    private fun stateOf(
        log: DayLog,
        prayer: Prayer,
        window: PrayerWindows.Window,
        now: LocalDateTime,
    ): RowState = when (log.answers[prayer]) {
        PrayerAnswer.YES -> RowState.PRAYED
        PrayerAnswer.NO -> RowState.NOT_PRAYED
        else -> when {
            window.contains(now) -> RowState.OPEN
            window.hasClosed(now) -> RowState.MISSED
            else -> RowState.UPCOMING
        }
    }

    /**
     * Records an answer from the screen. The window is checked again in the repository's caller
     * chain, but the button is only offered while the row is OPEN, so this is the honest path.
     */
    fun answer(prayer: Prayer, prayed: Boolean) {
        viewModelScope.launch {
            logRepository.answer(LocalDate.now(), prayer, prayed)
            // The question in the shade is now stale.
            TrackerNotifier.cancel(context, prayer)
            PrayerTimesWidgetReceiver.refresh(context)
        }
    }
}
