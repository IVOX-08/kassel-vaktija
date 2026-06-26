package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Android TV / Google TV variant — a big landscape prayer-times board for a wall display (Sony
 * Bravia etc.). Reuses [DashboardViewModel] (same times, Iqamah, Friday-Jumu'ah rule, next-prayer
 * highlight, live clock + countdown) and adds a daily "Hadith of the day" band along the bottom.
 * Always uses the light board palette so it matches the community design regardless of the TV theme.
 *
 * Sizing note: a 1080p TV is ~540dp tall, which is tight for emblem + hero + 6 prayer cards + Džuma
 * + the Hadith band. Fonts/cards are deliberately compact so nothing clips at that height; on roomier
 * (4K) panels the weight(1f) main area simply gets more breathing room. Verified at 540dp.
 */
private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val CARD_SHAPE = RoundedCornerShape(20.dp)

@Composable
fun TvDashboardScreen(modifier: Modifier = Modifier) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val hadithViewModel: TvHadithViewModel = hiltViewModel()
    val dailyHadith by hadithViewModel.daily.collectAsStateWithLifecycle()
    val showGerman by hadithViewModel.german.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { hadithViewModel.start() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackgroundLight)
            .padding(horizontal = 36.dp, vertical = 22.dp), // overscan-safe margin
    ) {
        if (state.loading) {
            Image(
                painter = painterResource(R.drawable.logo_emblem),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier.align(Alignment.Center).size(200.dp),
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                        modifier = Modifier.height(104.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    HeroCard(state, Modifier.fillMaxWidth())
                }

                // ---- Right column: header + 2-column prayer grid + Džuma ----
                Column(modifier = Modifier.fillMaxHeight().weight(0.71f)) {
                    Text(
                        text = "IGBD",
                        color = BrandGreenDark,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.header_subtitle),
                        color = BrandGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    ) {
                        state.rows.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { row -> TvPrayerCard(row, Modifier.weight(1f).heightIn(min = 78.dp)) }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        state.jumua?.let { JumuaCard(it, Modifier.fillMaxWidth()) }
                    }
                }
            }
            dailyHadith?.let {
                Spacer(Modifier.height(10.dp))
                DailyHadithBand(it, german = showGerman, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HeroCard(state: DashboardUiState, modifier: Modifier) {
    val weekday = state.gregorianDate.substringBefore(", ", state.gregorianDate)
    val dateOnly = state.gregorianDate.substringAfter(", ", "")
    Column(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(BrandGreen)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            state.clock,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(4.dp))
        Text(weekday, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (dateOnly.isNotEmpty()) {
            Text(dateOnly, color = BrandGoldLight, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Text(state.hijriDate, color = BrandGoldLight, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BrandGreenDark)
                .padding(vertical = 10.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_next_prayer_in),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(state.countdown, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun TvPrayerCard(row: PrayerRowUi, modifier: Modifier) {
    val on = row.isHighlighted
    val nameColor = if (on) Color.White else BrandGreen
    val iqamahColor = if (on) BrandGoldLight else BrandGold
    Column(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(if (on) BrandGreen else Color.White)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // The TV board uses the universal transliterated names (Fajr/Dhuhr/Asr/Maghrib/Isha, Jumu'ah
        // on Friday) for the prayers; only Sunrise keeps its localized word. This matches mosque-board
        // convention and keeps every name short so nothing truncates next to the time.
        val label = when {
            row.labelRes == R.string.prayer_jumua -> stringResource(R.string.tv_prayer_jumua)
            row.prayer == Prayer.FAJR -> stringResource(R.string.tv_prayer_fajr)
            row.prayer == Prayer.SUNRISE -> stringResource(R.string.prayer_sunrise)
            row.prayer == Prayer.DHUHR -> stringResource(R.string.tv_prayer_dhuhr)
            row.prayer == Prayer.ASR -> stringResource(R.string.tv_prayer_asr)
            row.prayer == Prayer.MAGHRIB -> stringResource(R.string.tv_prayer_maghrib)
            row.prayer == Prayer.ISHA -> stringResource(R.string.tv_prayer_isha)
            else -> stringResource(row.labelRes)
        }
        // Safety net: only the localized Sunrise word can get long; step it down if a locale is wordy.
        val nameSize = if (label.length >= 14) 16.sp else 20.sp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Text(row.adhan.format(HM), color = nameColor, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    stringResource(R.string.label_iqamah),
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

@Composable
private fun JumuaCard(jumua: LocalTime, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.tv_prayer_jumua), color = BrandGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(jumua.format(HM), color = BrandGreen, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * "Hadith of the day" band across the bottom of the TV board. The chosen daily hadith alternates
 * between Bosnian and German every minute (label + text together). Capped to two lines so it never
 * crowds the prayer grid; the full text lives in the in-app Hadith section.
 */
@Composable
private fun DailyHadithBand(daily: TvHadithViewModel.DailyHadith, german: Boolean, modifier: Modifier) {
    val text = if (german) daily.de else daily.bs
    if (text.isBlank()) return
    Column(
        modifier = modifier
            .clip(CARD_SHAPE)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = if (german) "Hadith des Tages" else "Hadis dana",
            color = BrandGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = text,
            color = BrandGreenDark,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
