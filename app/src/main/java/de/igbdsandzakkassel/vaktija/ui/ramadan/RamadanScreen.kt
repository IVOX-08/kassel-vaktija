package de.igbdsandzakkassel.vaktija.ui.ramadan

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldLight
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Ramadan screen: a Hijri Ramadan-day badge (or a days-until-Ramadan count outside Ramadan), a filling
 * progress ring with the live countdown to Iftar (or to the end of Suhoor at night), today's Sehur /
 * Imsak / Iftar / Tarawih times, the authentic Iftar supplication, and a private fasting tracker.
 */
@Composable
fun RamadanScreen(
    modifier: Modifier = Modifier,
    viewModel: RamadanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ramadan_prefs", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now() }
    var fasted by rememberSaveable { mutableStateOf(prefs.getBoolean("f_$today", false)) }
    LaunchedEffect(fasted) { prefs.edit().putBoolean("f_$today", fasted).apply() }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Days 1..ramadanDay map to the dates today-(ramadanDay-1) .. today; count the ones marked fasted.
    val fastedCount = remember(state.ramadanDay, fasted) {
        if (state.ramadanDay <= 0) 0
        else (0 until state.ramadanDay).count { i ->
            prefs.getBoolean("f_${today.minusDays((state.ramadanDay - 1 - i).toLong())}", false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RamadanBadge(state)
        FastingHero(state)
        TimesCard(state)
        IftarDuaCard()
        FastedCard(state, fastedCount, fasted) { fasted = it }
    }
}

@Composable
private fun RamadanBadge(state: RamadanUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🌙", fontSize = 26.sp) // 🌙
        Text(
            text = if (state.isRamadan) {
                stringResource(R.string.ramadan_day_badge, state.ramadanDay, state.hijriYear)
            } else {
                stringResource(R.string.ramadan_starts_in_days, state.daysUntilRamadan)
            },
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrandGreen,
        )
    }
    if (state.isRamadan) {
        Text(
            text = stringResource(R.string.ramadan_mubarak),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = BrandGold,
        )
    }
}

@Composable
private fun FastingHero(state: RamadanUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrandGreen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(208.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(
                        color = Color.White.copy(alpha = 0.22f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = BrandGoldLight,
                        startAngle = -90f, sweepAngle = 360f * state.progress.coerceIn(0f, 1f), useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(state.countdownLabelRes),
                        color = BrandGoldLight,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.countdown,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimesCard(state: RamadanUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeCol("🌙", stringResource(R.string.ramadan_sehur), state.sehurEnd, stringResource(R.string.ramadan_imsak)) // 🌙
            VerticalDivider(Modifier.height(56.dp))
            TimeCol("🌇", stringResource(R.string.ramadan_iftar), state.iftar, null) // 🌇
            VerticalDivider(Modifier.height(56.dp))
            TimeCol("🕌", stringResource(R.string.ramadan_teravija), state.teravija, null) // 🕌
        }
    }
}

@Composable
private fun RowScope.TimeCol(emoji: String, label: String, time: LocalTime, hint: String?) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = time.format(HM),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrandGreen,
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IftarDuaCard() {
    // For an Arabic UI the original IS the text — skip the transliteration + translation (like Dhikr).
    val arabicUi = LocalConfiguration.current.locales[0].language == "ar"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.ramadan_dua_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = stringResource(R.string.ramadan_dua_ar),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (!arabicUi) {
                Text(
                    text = stringResource(R.string.ramadan_dua_translit),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = BrandGold,
                )
                Text(
                    text = stringResource(R.string.ramadan_dua_meaning),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FastedCard(
    state: RamadanUiState,
    fastedCount: Int,
    fasted: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (state.isRamadan) {
                Text(
                    text = stringResource(R.string.ramadan_fasted_count, fastedCount, state.ramadanLength),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!fasted) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ramadan_fasted_today),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = fasted, onCheckedChange = onToggle)
            }
        }
    }
}
