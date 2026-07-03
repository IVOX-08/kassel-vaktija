package de.igbdsandzakkassel.vaktija.ui.calendar

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    val listState = rememberLazyListState()
    val context = LocalContext.current
    // "Remove animations" accessibility setting → jump to today instead of animate-scroll + pulse.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val todayIndex = state.days.indexOfFirst { it.date == today }
    // Guard so we scroll/pulse once per visit to today's month (not on every recomposition). Uses
    // `remember` (not Saveable) so re-opening the Calendar tab scrolls to today and pulses again.
    var pulsedForMonth by remember { mutableStateOf<String?>(null) }
    var pulseToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.loading, state.month, todayIndex) {
        if (state.loading || todayIndex < 0) return@LaunchedEffect // today not in the shown month
        val monthKey = state.month.toString()
        if (pulsedForMonth == monthKey) return@LaunchedEffect

        // Wait until the LazyColumn is actually laid out. The effect fires the instant `loading`
        // flips to false — before the list has been measured — so scrolling here was a no-op and
        // today stayed off-screen. Suspend until the list reports its items, THEN scroll.
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
        pulsedForMonth = monthKey

        if (reduceMotion) {
            listState.scrollToItem(todayIndex)
            return@LaunchedEffect
        }
        // Gentle scroll, then center today's row in the viewport, then a one-shot illumination pulse.
        listState.animateScrollToItem(todayIndex)
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == todayIndex }
        if (item != null) {
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            val itemCenter = item.offset + item.size / 2f
            listState.animateScrollBy(itemCenter - viewportCenter)
        }
        pulseToken++
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        MonthNavHeader(
            title = monthTitle(state.month, locale),
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            // Quick way home after paging months (arrows only move one month at a time).
            showToday = state.month != java.time.YearMonth.now(),
            onToday = viewModel::goToToday,
        )
        ColumnHeaderRow()
        HorizontalDivider()

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(state.days, key = { it.date.toString() }) { day ->
                    val isToday = day.date == today
                    DayRow(
                        day = day,
                        isToday = isToday,
                        locale = locale,
                        pulseToken = if (isToday) pulseToken else 0,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun MonthNavHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    showToday: Boolean,
    onToday: () -> Unit,
) {
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
        if (showToday) {
            androidx.compose.material3.TextButton(onClick = onToday) {
                Text(stringResource(R.string.calendar_today), fontWeight = FontWeight.SemiBold)
            }
        }
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
private fun DayRow(day: DailyTimes, isToday: Boolean, locale: Locale, pulseToken: Int = 0) {
    // One-shot illumination when today is auto-scrolled into view: the green briefly brightens, then
    // settles back to the persistent today highlight.
    val glow = remember { Animatable(0f) }
    LaunchedEffect(pulseToken) {
        if (pulseToken > 0) {
            glow.snapTo(0f)
            glow.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            glow.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
        }
    }
    val baseAlpha = if (isToday) 0.14f else 0f
    val background =
        if (isToday || glow.value > 0f) {
            MaterialTheme.colorScheme.primary.copy(alpha = baseAlpha + glow.value * 0.34f)
        } else {
            Color.Transparent
        }
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
