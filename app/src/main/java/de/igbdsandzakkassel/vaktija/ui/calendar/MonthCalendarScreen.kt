package de.igbdsandzakkassel.vaktija.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DATE_WEIGHT = 1.5f
private const val TIME_WEIGHT = 1f
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun LocalTime.hhmm(): String = format(TIME)

@Composable
fun MonthCalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: MonthCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()
    val today = LocalDate.now()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        MonthNavHeader(
            title = monthTitle(state.month, locale),
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
        )
        ColumnHeaderRow()
        HorizontalDivider()

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.days, key = { it.date.toString() }) { day ->
                    DayRow(day = day, isToday = day.date == today, locale = locale)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun MonthNavHeader(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_prev_month))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_next_month))
        }
    }
}

@Composable
private fun ColumnHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("", DATE_WEIGHT)
        HeaderCell(stringResource(R.string.prayer_abbr_fajr), TIME_WEIGHT)
        HeaderCell(stringResource(R.string.prayer_abbr_sunrise), TIME_WEIGHT)
        HeaderCell(stringResource(R.string.prayer_abbr_dhuhr), TIME_WEIGHT)
        HeaderCell(stringResource(R.string.prayer_abbr_asr), TIME_WEIGHT)
        HeaderCell(stringResource(R.string.prayer_abbr_maghrib), TIME_WEIGHT)
        HeaderCell(stringResource(R.string.prayer_abbr_isha), TIME_WEIGHT)
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun DayRow(day: DailyTimes, isToday: Boolean, locale: Locale) {
    val background = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    val timeColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val timeWeight = if (isToday) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(DATE_WEIGHT), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = day.date.format(DateTimeFormatter.ofPattern("EEE", locale)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TimeCell(day.fajr, timeColor, timeWeight)
        TimeCell(day.sunrise, timeColor, timeWeight)
        TimeCell(day.dhuhr, timeColor, timeWeight)
        TimeCell(day.asr, timeColor, timeWeight)
        TimeCell(day.maghrib, timeColor, timeWeight)
        TimeCell(day.isha, timeColor, timeWeight)
    }
}

@Composable
private fun RowScope.TimeCell(time: LocalTime, color: Color, weight: FontWeight) {
    Text(
        text = time.hhmm(),
        fontSize = 13.sp,
        fontWeight = weight,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(TIME_WEIGHT),
    )
}

private fun monthTitle(month: YearMonth, locale: Locale): String =
    month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
