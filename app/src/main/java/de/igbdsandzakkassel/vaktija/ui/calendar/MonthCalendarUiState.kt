package de.igbdsandzakkassel.vaktija.ui.calendar

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import java.time.YearMonth

data class MonthCalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<DailyTimes> = emptyList(),
    val loading: Boolean = true,
)
