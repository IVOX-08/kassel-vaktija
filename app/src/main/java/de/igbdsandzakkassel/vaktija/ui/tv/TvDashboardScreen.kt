package de.igbdsandzakkassel.vaktija.ui.tv

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.ui.dashboard.DashboardUiState
import de.igbdsandzakkassel.vaktija.ui.dashboard.DashboardViewModel
import de.igbdsandzakkassel.vaktija.ui.dashboard.PrayerRowUi
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldLight
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreenDark
import de.igbdsandzakkassel.vaktija.ui.theme.PageBackgroundLight
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Android TV / Google TV variant — a big landscape prayer-times board for a wall display in the
 * mosque entrance: people glance at it to see when the Adhan is and read a hadith. It is purely
 * passive (no remote interaction, and no Adhan sound — alarms are skipped on TV).
 *
 * The whole board automatically alternates between the community's two languages, **German and
 * Bosnian**, with a gentle crossfade so it's clearly noticeable that it switched:
 *  - the prayer board (names, labels, weekday/date, countdown caption) flips every [BOARD_SWITCH_MS];
 *  - the "Hadith of the day" band flips on its own ~1-minute rhythm (a hadith needs longer to read).
 * In German the prayers use the transliterated names (Fajr/Dhuhr/Asr/Maghrib/Isha, Jumu'ah) with the
 * German "Sonnenaufgang"; in Bosnian everything is Bosnian (Sabah/Podne/Ikindija/Akšam/Jacija,
 * Izlazak sunca, IKAMET, petak…). The system locale is ignored — only these two languages show.
 */
private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val GERMAN: Locale = Locale.GERMAN
private val BOSNIAN: Locale = Locale.forLanguageTag("bs")
private const val BOARD_SWITCH_MS = 5_000L
private const val CROSSFADE_MS = 700

@Composable
fun TvDashboardScreen(modifier: Modifier = Modifier) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val hadithViewModel: TvHadithViewModel = hiltViewModel()
    val dailyHadith by hadithViewModel.daily.collectAsStateWithLifecycle()
    val hadithGerman by hadithViewModel.german.collectAsStateWithLifecycle()
    val hadithSecondsLeft by hadithViewModel.secondsLeft.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { hadithViewModel.start() }

    // Wall board: keep the TV screen awake the whole time the app is shown (no screensaver/sleep).
    val keepAwakeView = LocalView.current
    DisposableEffect(Unit) {
        keepAwakeView.keepScreenOn = true
        onDispose { keepAwakeView.keepScreenOn = false }
    }

    val context = LocalContext.current
    // Keyed on context so a config change (density/font scale) rebuilds them instead of going stale.
    val deCtx = remember(context) { context.localized(GERMAN) }
    val bsCtx = remember(context) { context.localized(BOSNIAN) }

    // Flip the board language every few seconds (the hadith band has its own slower rhythm).
    var boardGerman by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(BOARD_SWITCH_MS)
            boardGerman = !boardGerman
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackgroundLight)
            .padding(horizontal = 36.dp, vertical = 22.dp), // overscan-safe margin
    ) {
        if (state.loading) {
            // First-ever start with an empty cache: don't sit on a silent logo forever — after a
            // few seconds tell the installer what's wrong (usually: TV not on Wi-Fi yet).
            var showHint by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(6_000)
                showHint = true
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_emblem),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier.size(200.dp),
                )
                if (showHint) {
                    Text(
                        text = "Gebetszeiten werden geladen … bitte Internetverbindung prüfen\nUčitavanje namaskih vremena … provjeri internet vezu",
                        color = BrandGreenDark,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = boardGerman,
                animationSpec = tween(CROSSFADE_MS),
                modifier = Modifier.weight(1f),
                label = "boardLanguage",
            ) { german ->
                BoardBody(state, german, if (german) deCtx else bsCtx)
            }
            // Public wall display MUST say when it could not refresh — otherwise days-old times
            // (which drift 1-2 min/day) would be presented as today's without any hint.
            if (state.isStale) {
                Text(
                    text = "⚠ Vremena možda nisu ažurna (nema interneta) · Zeiten evtl. nicht aktuell (kein Internet)",
                    color = BrandGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            dailyHadith?.let { hadith ->
                Spacer(Modifier.height(10.dp))
                Crossfade(
                    targetState = hadithGerman,
                    animationSpec = tween(CROSSFADE_MS),
                    label = "hadithLanguage",
                ) { german ->
                    DailyHadithBand(hadith, german, hadithSecondsLeft, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BoardBody(state: DashboardUiState, german: Boolean, ctx: Context) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // ---- Left column: emblem + green hero (clock, dates, countdown) ----
        Column(
            modifier = Modifier.fillMaxHeight().weight(0.29f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_emblem),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier.height(80.dp),
            )
            Spacer(Modifier.height(6.dp))
            HeroCard(state, german, ctx, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            // Growth loop: everyone waiting at the entrance can scan the board to install the app.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.tv_qr_play),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "Skeniraj za aplikaciju\nApp scannen",
                    color = BrandGreenDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // ---- Right column: header + 2-column prayer grid + Džuma ----
        Column(modifier = Modifier.fillMaxHeight().weight(0.71f)) {
            Text(
                text = "IGBD",
                color = BrandGreenDark,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                // The big "IGBD" is already above; the subtitle is just the community name — no
                // repeated "IGBD-", and "Sandžak Kassel" with a space (not a hyphen).
                text = "Gemeinde Sandžak Kassel",
                color = BrandGold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                state.rows.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { row ->
                            TvPrayerCard(row, german, ctx, Modifier.weight(1f).heightIn(min = 70.dp))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                state.jumua?.let { JumuaCard(it, german, ctx, Modifier.fillMaxWidth()) }
                // Admin-announced Eid prayer — gold banner on the wall board until the day passes.
                state.bajram?.let { (date, time) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CARD_SHAPE)
                            .background(BrandGold)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "🌙 " + ctx.getString(R.string.bajram_prayer) + " · " +
                                date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", if (german) GERMAN else BOSNIAN)),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = time.format(HM),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(state: DashboardUiState, german: Boolean, ctx: Context, modifier: Modifier) {
    val locale = if (german) GERMAN else BOSNIAN
    val today = LocalDate.now()
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE", locale))
    val dateLine = today.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", locale))
    Column(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(BrandGreen)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The live clock is the most-glanced element on the wall board → make it the biggest.
        Text(state.clock, color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Spacer(Modifier.height(3.dp))
        // Weekday + date on ONE line; the Hijri date sits just below it.
        Text(
            text = "$weekday, $dateLine",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        // Format the Hijri date HERE with the board's alternating de/bs locale — state.hijriDate is
        // rendered in the SYSTEM locale, which would leak a third language onto the bilingual board.
        Text(
            text = java.time.chrono.HijrahDate.from(today).format(
                DateTimeFormatter.ofPattern("d. MMMM yyyy G", locale),
            ),
            color = BrandGoldLight,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BrandGreenDark)
                .padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = ctx.getString(R.string.dashboard_next_prayer_in),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            // Name the prayer being counted down to — the most-glanced element must be
            // self-contained (readable without scanning the grid for the highlighted card).
            state.rows.firstOrNull { it.isHighlighted }?.let { next ->
                Text(
                    text = prayerLabel(next, german, ctx),
                    color = BrandGoldLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(state.countdown, color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun TvPrayerCard(row: PrayerRowUi, german: Boolean, ctx: Context, modifier: Modifier) {
    // Filled green look while it's the next prayer OR currently in its Adhan→Iqamah window.
    val on = row.isHighlighted || row.inIqamahWindow
    // Prayer NAME is gold (owner's reference design); the TIME stays green / white-on-green.
    val nameColor = if (on) BrandGoldLight else BrandGold
    val timeColor = if (on) Color.White else BrandGreen
    val iqamahColor = if (on) BrandGoldLight else BrandGold
    val label = prayerLabel(row, german, ctx)
    val nameSize = if (label.length >= 14) 16.sp else 20.sp
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
    Column(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(if (on) BrandGreen else Color.White)
            .then(
                if (row.inIqamahWindow) {
                    Modifier.border(
                        (2f + breath.value * 3f).dp,
                        Color.White.copy(alpha = 0.55f + breath.value * 0.45f),
                        CARD_SHAPE,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = nameColor,
                fontSize = nameSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(row.adhan.format(HM), color = timeColor, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (row.iqamah != null) {
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = if (on) Color.White.copy(alpha = 0.35f) else BrandGreen.copy(alpha = 0.15f))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ctx.getString(R.string.label_iqamah),
                    color = if (on) Color.White.copy(alpha = 0.9f) else BrandGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(row.iqamah.format(HM), color = iqamahColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/**
 * The prayer's name for the board. In German: the transliterated name (Fajr/Dhuhr/Asr/Maghrib/Isha,
 * Jumu'ah on Friday) with the German word for Sunrise. In Bosnian: the Bosnian name straight from the
 * (Bosnian-context) resources — Sabah/Podne/Ikindija/Akšam/Jacija, Izlazak sunca, Džuma namaz.
 */
private fun prayerLabel(row: PrayerRowUi, german: Boolean, ctx: Context): String =
    if (german) {
        when {
            row.labelRes == R.string.prayer_jumua -> ctx.getString(R.string.tv_prayer_jumua)
            row.prayer == Prayer.SUNRISE -> ctx.getString(R.string.prayer_sunrise)
            row.prayer == Prayer.FAJR -> ctx.getString(R.string.tv_prayer_fajr)
            row.prayer == Prayer.DHUHR -> ctx.getString(R.string.tv_prayer_dhuhr)
            row.prayer == Prayer.ASR -> ctx.getString(R.string.tv_prayer_asr)
            row.prayer == Prayer.MAGHRIB -> ctx.getString(R.string.tv_prayer_maghrib)
            row.prayer == Prayer.ISHA -> ctx.getString(R.string.tv_prayer_isha)
            else -> ctx.getString(row.labelRes)
        }
    } else {
        ctx.getString(row.labelRes)
    }

@Composable
private fun JumuaCard(jumua: java.time.LocalTime, german: Boolean, ctx: Context, modifier: Modifier) {
    val label = if (german) ctx.getString(R.string.tv_prayer_jumua) else ctx.getString(R.string.prayer_jumua)
    Row(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = BrandGold, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(jumua.format(HM), color = BrandGreen, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * "Hadith of the day" band across the bottom. [german] true → the German text + "Hadith des Tages",
 * else the Bosnian text + "Hadis dana". Capped to two lines so it never crowds the prayer grid; the
 * full text lives in the in-app Hadith section.
 */
@Composable
private fun DailyHadithBand(
    daily: TvHadithViewModel.DailyHadith,
    german: Boolean,
    secondsLeft: Int,
    modifier: Modifier,
) {
    val text = if (german) daily.de else daily.bs
    if (text.isBlank()) return
    Column(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (german) "Hadith des Tages" else "Hadis dana",
                color = BrandGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            // Small hint on the right: which language comes next + seconds until it switches.
            Text(
                text = if (german) "Bosanski za: ${secondsLeft}s" else "Deutsch in: ${secondsLeft}s",
                color = BrandGold.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = text,
            color = BrandGreenDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 21.sp,
            // Bigger, well-readable text (the board's top area was compacted to free this room) with up
            // to 4 lines so a complete short/medium hadith shows in FULL (no "…"). The curated board
            // hadiths (board.json) are kept short enough that this never pushes Jumu'ah off-screen.
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A context whose resources resolve strings/formatters in [locale], regardless of the system locale. */
private fun Context.localized(locale: Locale): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
