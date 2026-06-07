package de.igbdsandzakkassel.vaktija.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldLight
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun LocalTime.hhmm(): String = format(TIME)

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(state, modifier)
}

@Composable
private fun DashboardContent(state: DashboardUiState, modifier: Modifier = Modifier) {
    if (state.loading) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isStale) item { StaleBanner() }
        item { Header(state.gregorianDate, state.hijriDate) }
        item { CountdownCard(state) }
        items(state.rows, key = { it.prayer }) { PrayerCard(it) }
        state.jumua?.let { jumua -> item { JumuaCard(jumua) } }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StaleBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = BrandGold,
    ) {
        Text(
            text = stringResource(R.string.cache_stale_warning),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun Header(gregorianDate: String, hijriDate: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_community),
            contentDescription = stringResource(R.string.cd_app_logo),
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(110.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = gregorianDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = hijriDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

/** Green hero card: only the "next prayer in" label + live countdown. */
@Composable
private fun CountdownCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_next_prayer_in) + ":",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandGoldLight,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.countdown,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun PrayerCard(row: PrayerRowUi) {
    val container by animateColorAsState(
        targetValue = if (row.isHighlighted) BrandGreen else MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label = "prayerCardContainer",
    )
    val accent = if (row.isHighlighted) Color.Transparent else MaterialTheme.colorScheme.primary
    val nameColor = if (row.isHighlighted) BrandGoldLight else MaterialTheme.colorScheme.secondary
    val adhanColor = if (row.isHighlighted) Color.White else MaterialTheme.colorScheme.primary
    val iqamahLabelColor =
        if (row.isHighlighted) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iqamahColor = if (row.isHighlighted) BrandGoldLight else MaterialTheme.colorScheme.secondary
    val dividerColor =
        if (row.isHighlighted) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = container,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Green left accent bar (transparent on the highlighted/filled card).
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 12.dp, bottom = 12.dp)
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Prayer (Adhan) row
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(row.prayer.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = nameColor,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = row.adhan.hhmm(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = adhanColor,
                    )
                }
                // Divider + Iqamah row (only for prayers that have an Iqamah)
                if (row.iqamah != null) {
                    HorizontalDivider(thickness = 1.dp, color = dividerColor)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.label_iqamah),
                            style = MaterialTheme.typography.labelMedium,
                            color = iqamahLabelColor,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = row.iqamah.hhmm(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = iqamahColor,
                        )
                    }
                }
            }
        }
    }
}

/** Full-width Friday prayer card, distinguished by a gold accent bar. */
@Composable
private fun JumuaCard(time: LocalTime) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 12.dp, bottom = 12.dp)
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(BrandGold),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.prayer_jumua),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = time.hhmm(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
