package de.igbdsandzakkassel.vaktija.ui.dashboard

import de.igbdsandzakkassel.vaktija.core.text.ltr
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.Mosque
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.CommunityStatus
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldLight
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How tall a community's mark is drawn — the same for the home crest and the federation logo. */
private val EMBLEM_HEIGHT = 96.dp

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun LocalTime.hhmm(): String = format(TIME)

// TBD-community-rule: confirm the donation target. For now this opens PayPal's donate flow for the
// community's PayPal e-mail. A hosted PayPal donate button or a paypal.me handle would give a nicer
// flow if the community sets one up.
private const val PAYPAL_URL =
    "https://www.paypal.com/donate/?business=ikzsandzakkassel@gmail.com&currency_code=EUR"
/** Maps search for whichever town is selected — the address is no longer a fixed string. */
private fun mapsUrl(query: String): String =
    "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)

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
        // First-ever launch with no cache: after a few seconds explain WHY it's still loading
        // (almost always: no internet yet) instead of spinning silently forever.
        var showHint by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(6_000)
            showHint = true
        }
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            if (showHint) {
                Text(
                    text = stringResource(R.string.offline_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
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
        Header(state, state.gregorianDate, state.hijriDate)
        // Pinned, not a list item: the list auto-scrolls to the next prayer on open, which would
        // carry this notice off the top of the screen exactly when it needs to be read.
        if (state.communityStatus == CommunityStatus.SUSPENDED) {
            InactiveCommunityBanner(modifier = Modifier.padding(horizontal = 16.dp))
        }
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
            // Admin-announced Eid prayer — the community's most-asked question, so it leads the list.
            state.bajram?.let { (date, time) -> item { BajramCard(date, time) } }
            items(state.rows, key = { it.prayer }) { row ->
                PrayerCard(row, pulseToken = if (row.isHighlighted) pulseToken else 0)
            }
            state.jumua?.let { jumua -> item { JumuaCard(jumua) } }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/**
 * Shown when the head admin has switched this community off. Deliberately points at Settings: the
 * ordinary member did nothing wrong and needs a way out, not just a notice.
 */
@Composable
private fun InactiveCommunityBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = BrandGold,
    ) {
        Text(
            text = stringResource(R.string.community_inactive_notice),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
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
private fun Header(state: DashboardUiState, gregorianDate: String, hijriDate: String) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: mosque address → opens Maps. LRI/PDI isolates guarantee the digit-heavy LTR
            // address renders in order under Arabic/Urdu (same treatment as the Settings screen).
            // Falls back to the town name while a community still has no street address on file,
            // so the header never sits empty for a newly-added community.
            val address = state.locationAddress ?: state.locationName
            HeaderSideItem(
                icon = Icons.Filled.Place,
                label = ltr(address),
                emphasized = false,
                alignment = Alignment.Start,
                onClick = { if (address.isNotBlank()) uriHandler.openUri(mapsUrl(address)) },
                modifier = Modifier.weight(1f),
            )
            // Center: community emblem, blended into the page background (like the website logo).
            CommunityEmblem(state)
            // Right: donate → opens PayPal. Hidden entirely while the community is switched off —
            // leaving the button would keep collecting for a community that is no longer listed,
            // and falling back to the old built-in link would send the money to Kassel.
            val donationUrl = state.donationUrl
            if (donationUrl == null) {
                Spacer(Modifier.weight(1f))
            } else HeaderSideItem(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.action_donate),
                emphasized = true,
                alignment = Alignment.End,
                onClick = { uriHandler.openUri(donationUrl) },
                modifier = Modifier.weight(1f),
            )
        }
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

/**
 * A tappable, centred icon + text block flanking the emblem (mosque address / donate). When
 * [emphasized] (donate), the icon and word are larger and brand-green so the call to donate stands out.
 */
@Composable
private fun HeaderSideItem(
    icon: ImageVector,
    label: String,
    emphasized: Boolean,
    alignment: Alignment.Horizontal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Outer column positions the block toward the screen edge (Start = address/left, End = donate/right);
    // the inner column keeps the icon centred over its text.
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (emphasized) 30.dp else 22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The community emblem drawn so its backdrop blends into the page background. In light mode we draw
 * with [BlendMode.Multiply] (like the website's `mix-blend-mode: multiply`), which turns the emblem's
 * white backdrop into the grey page colour. In dark mode the emblem already sits on black, so we draw
 * it normally.
 */
/**
 * The selected community's emblem.
 *
 * Kassel keeps its own coat of arms, blended into the page the way the website shows it. Every
 * other community carries the federation's mark with its own name underneath — they belong to
 * IGBD, and none of them has a logo on file. A generic mosque glyph said nothing about whose
 * times were on screen; the name does.
 */
@Composable
private fun CommunityEmblem(state: DashboardUiState, modifier: Modifier = Modifier) {
    if (state.isHomeCommunity) {
        BlendedEmblem(modifier.height(EMBLEM_HEIGHT))
        return
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_igbd),
            contentDescription = stringResource(R.string.cd_app_logo),
            contentScale = ContentScale.Fit,
            // The same height as Kassel's crest. Weighting it inside a fixed box made every other
            // community's mark visibly smaller than the home one, which read as second class.
            modifier = Modifier.height(EMBLEM_HEIGHT),
        )
        Text(
            text = state.communityName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = BrandGreen,
            textAlign = TextAlign.Center,
            // Two lines: names like "Islamski kulturni centar Bošnjaka u Berlinu" do not fit on
            // one, and cutting a community's name down to an ellipsis is not a good greeting.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun BlendedEmblem(modifier: Modifier = Modifier) {
    val emblem = ImageBitmap.imageResource(R.drawable.logo_community)
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val description = stringResource(R.string.cd_app_logo)
    Canvas(
        modifier = modifier
            .aspectRatio(emblem.width.toFloat() / emblem.height.toFloat())
            .semantics { contentDescription = description },
    ) {
        drawImage(
            image = emblem,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(emblem.width, emblem.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            blendMode = if (isLight) BlendMode.Multiply else BlendMode.SrcOver,
        )
    }
}

/** Green hero card: only the "next prayer in" label + live countdown. */
@Composable
private fun CountdownCard(state: DashboardUiState, modifier: Modifier = Modifier) {
    Card(
        // Slightly narrower than the prayer cards (extra horizontal inset) and a touch shorter.
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_next_prayer_in) + ":",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandGoldLight,
            )
            // Name WHICH prayer the countdown targets — the pinned card must be self-contained
            // (the highlighted list card can be scrolled off-screen).
            state.nextPrayerLabelRes?.let { labelRes ->
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.countdown,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun PrayerCard(row: PrayerRowUi, pulseToken: Int = 0) {
    // Filled green look while it's the next prayer OR currently in its Adhan→Iqamah window.
    val active = row.isHighlighted || row.inIqamahWindow
    val container by animateColorAsState(
        targetValue = if (active) BrandGreen else MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label = "prayerCardContainer",
    )
    val accent = if (active) Color.Transparent else MaterialTheme.colorScheme.primary
    val nameColor = if (active) BrandGoldLight else MaterialTheme.colorScheme.secondary
    val adhanColor = if (active) Color.White else MaterialTheme.colorScheme.primary
    val iqamahLabelColor =
        if (active) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
    val iqamahColor = if (active) BrandGoldLight else MaterialTheme.colorScheme.secondary
    val dividerColor =
        if (active) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant

    // One-shot illumination pulse when this becomes the active card (token increments once).
    val glow = remember { Animatable(0f) }
    LaunchedEffect(pulseToken) {
        if (pulseToken > 0) {
            glow.snapTo(0f)
            glow.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            glow.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    // Continuous green-white "breathing" glow for the whole Adhan→Iqamah window.
    val breath = remember { Animatable(0f) }
    LaunchedEffect(row.inIqamahWindow) {
        if (row.inIqamahWindow) {
            while (true) {
                breath.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
                breath.animateTo(0f, tween(950, easing = FastOutSlowInEasing))
            }
        } else {
            breath.snapTo(0f)
        }
    }
    val glowValue = glow.value
    val breathValue = breath.value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = container,
        shadowElevation = 2.dp + (maxOf(glowValue, breathValue) * 12f).dp,
        border = when {
            row.inIqamahWindow ->
                BorderStroke((2f + breathValue * 3f).dp, Color.White.copy(alpha = 0.55f + breathValue * 0.45f))
            glowValue > 0.01f ->
                BorderStroke((2.5f * glowValue).dp, BrandGoldLight.copy(alpha = glowValue))
            else -> null
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
                        text = stringResource(row.labelRes),
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
private fun BajramCard(date: java.time.LocalDate, time: LocalTime) {
    // Festive gold banner for the admin-announced Eid prayer (auto-hides once the day has passed).
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🌙 " + stringResource(R.string.bajram_prayer),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = date.format(
                        DateTimeFormatter.ofPattern(
                            android.text.format.DateFormat.getBestDateTimePattern(
                                Locale.getDefault(), "EEEEdMMMM",
                            ),
                            Locale.getDefault(),
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = time.hhmm(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

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
