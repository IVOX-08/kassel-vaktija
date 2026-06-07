package de.igbdsandzakkassel.vaktija.ui.dashboard

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

    val listState = rememberLazyListState()
    val context = LocalContext.current
    // "Remove animations" accessibility setting → skip the pulse and jump instead of animate-scroll.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val highlightedName = state.rows.firstOrNull { it.isHighlighted }?.prayer?.name
    val highlightIndexInRows = state.rows.indexOfFirst { it.isHighlighted }
    // Lazy index of the highlighted prayer row. Only the optional stale banner precedes it now —
    // the Header and countdown are pinned above the list, not list items.
    val targetIndex = if (highlightIndexInRows >= 0) (if (state.isStale) 1 else 0) + highlightIndexInRows else -1

    // Guard against replays: only animate on first entry and when the active prayer actually changes.
    var lastAnimated by rememberSaveable { mutableStateOf<String?>(null) }
    var pulseToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(highlightedName, state.loading) {
        if (state.loading || highlightedName == null || targetIndex < 0) return@LaunchedEffect
        if (lastAnimated == highlightedName) return@LaunchedEffect
        lastAnimated = highlightedName

        if (reduceMotion) {
            listState.scrollToItem(targetIndex)
            return@LaunchedEffect
        }
        // Gentle scroll, then center the card in the viewport. (Programmatic — emits no pointer
        // events, so it never trips the user-touch detector and the hero stays visible.)
        listState.animateScrollToItem(targetIndex)
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (item != null) {
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            val itemCenter = item.offset + item.size / 2f
            listState.animateScrollBy(itemCenter - viewportCenter)
        }
        pulseToken++ // one-shot illumination on the highlighted card
    }

    Column(modifier = modifier.fillMaxSize()) {
        Header(state.gregorianDate, state.hijriDate)
        // Pinned above the list, always visible — never hides on touch/scroll.
        CountdownCard(state, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isStale) item { StaleBanner() }
            items(state.rows, key = { it.prayer }) { row ->
                PrayerCard(row, pulseToken = if (row.isHighlighted) pulseToken else 0)
            }
            state.jumua?.let { jumua -> item { JumuaCard(jumua) } }
            item { Spacer(Modifier.height(8.dp)) }
        }
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
private fun CountdownCard(state: DashboardUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
private fun PrayerCard(row: PrayerRowUi, pulseToken: Int = 0) {
    val container by animateColorAsState(
        targetValue = if (row.isHighlighted) BrandGreen else MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label = "prayerCardContainer",
    )
    val accent = if (row.isHighlighted) Color.Transparent else MaterialTheme.colorScheme.primary
    val nameColor = if (row.isHighlighted) BrandGoldLight else MaterialTheme.colorScheme.secondary
    val adhanColor = if (row.isHighlighted) Color.White else MaterialTheme.colorScheme.primary
    val iqamahLabelColor =
        if (row.isHighlighted) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
    val iqamahColor = if (row.isHighlighted) BrandGoldLight else MaterialTheme.colorScheme.secondary
    val dividerColor =
        if (row.isHighlighted) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant

    // One-shot illumination pulse when this becomes the active card (token increments once).
    val glow = remember { Animatable(0f) }
    LaunchedEffect(pulseToken) {
        if (pulseToken > 0) {
            glow.snapTo(0f)
            glow.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            glow.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    val glowValue = glow.value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = container,
        shadowElevation = 2.dp + (glowValue * 12f).dp,
        border = if (glowValue > 0.01f) {
            BorderStroke((2.5f * glowValue).dp, BrandGoldLight.copy(alpha = glowValue))
        } else {
            null
        },
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

/** Full-width Friday prayer card, distinguished by a green border (matches the website CSS). */
@Composable
private fun JumuaCard(time: LocalTime) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
