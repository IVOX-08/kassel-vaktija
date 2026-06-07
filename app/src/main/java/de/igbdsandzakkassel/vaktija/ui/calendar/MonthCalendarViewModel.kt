package de.igbdsandzakkassel.vaktija.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.repository.MonthCalendarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonthCalendarViewModel @Inject constructor(
    private val repository: MonthCalendarRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<MonthCalendarUiState> = month.flatMapLatest { ym ->
        flow {
            emit(MonthCalendarUiState(month = ym, loading = true))
            emit(MonthCalendarUiState(month = ym, days = repository.getMonth(ym), loading = false))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthCalendarUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
}
