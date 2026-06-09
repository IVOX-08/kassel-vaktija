package de.igbdsandzakkassel.vaktija.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.quran.Ayah
import de.igbdsandzakkassel.vaktija.data.quran.SurahMeta
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldLight
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen

private const val BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

/** Chapter list: all 114 surahs. */
@Composable
fun QuranListScreen(
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuranViewModel = hiltViewModel(),
) {
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val list = surahs
    if (list == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
    ) {
        items(list, key = { it.id }) { surah -> SurahRow(surah) { onOpen(surah.id) } }
    }
}

@Composable
private fun SurahRow(surah: SurahMeta, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandGreen.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("${surah.id}", color = BrandGreen, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(surah.transliteration, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.quran_verses, surah.totalVerses),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = surah.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
            )
        }
    }
}

/**
 * Surah reader: turn pages like a printed Mushaf. Ayahs are grouped by their official Mushaf page
 * (604-page Madani layout) and laid out as continuous justified Arabic; swipe right-to-left to turn
 * the page. No translations — Arabic only, by design.
 */
@Composable
fun QuranSurahScreen(
    surahId: Int,
    onBack: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel(),
) {
    LaunchedEffect(surahId) { viewModel.loadSurah(surahId) }
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    val ayahs by viewModel.ayahs.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                text = meta?.transliteration ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val list = ayahs
        if (list == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return
        }
        val pages = remember(list) { groupByPage(list) }
        val pagerState = rememberPagerState(pageCount = { pages.size })
        // RTL pager: swiping right-to-left advances the page, like a real Mushaf.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                val (pageNumber, pageAyahs) = pages[index]
                QuranPage(
                    surahId = surahId,
                    meta = meta,
                    isFirstPage = index == 0,
                    pageNumber = pageNumber,
                    ayahs = pageAyahs,
                )
            }
        }
    }
}

@Composable
private fun QuranPage(
    surahId: Int,
    meta: SurahMeta?,
    isFirstPage: Boolean,
    pageNumber: Int,
    ayahs: List<Ayah>,
) {
    val styled = buildAnnotatedString {
        ayahs.forEachIndexed { i, ayah ->
            append(ayah.text)
            append(" ")
            withStyle(SpanStyle(color = BrandGold, fontWeight = FontWeight.Bold)) {
                append("﴿${toArabicIndic(ayah.number)}﴾")
            }
            if (i != ayahs.lastIndex) append("  ")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        if (isFirstPage) {
            SurahTitleBlock(meta)
            if (surahId != 1 && surahId != 9) BismillahHeader()
        }
        // The ayah text auto-shrinks to fill the remaining height — the whole page fits, no scroll.
        AutoFitArabicText(
            text = styled,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Text(
            text = toArabicIndic(pageNumber),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

/** Renders Arabic text at the largest font size that still fits the available height (no scroll). */
@Composable
private fun AutoFitArabicText(text: AnnotatedString, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx().toInt() }
        val heightPx = with(density) { maxHeight.toPx() }
        val fontSp = remember(text, widthPx, heightPx) {
            var lo = 12f
            var hi = 30f
            var best = 12f
            repeat(11) {
                val mid = (lo + hi) / 2f
                val measured = measurer.measure(
                    text = text,
                    style = TextStyle(fontSize = mid.sp, lineHeight = (mid * 1.95f).sp, textAlign = TextAlign.Justify),
                    constraints = Constraints(maxWidth = widthPx),
                    layoutDirection = LayoutDirection.Rtl,
                )
                if (measured.size.height <= heightPx) {
                    best = mid
                    lo = mid
                } else {
                    hi = mid
                }
            }
            best
        }
        Text(
            text = text,
            fontSize = fontSp.sp,
            lineHeight = (fontSp * 1.95f).sp,
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SurahTitleBlock(meta: SurahMeta?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = BrandGreen,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = meta?.name ?: "",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGoldLight,
            )
            if (meta != null) {
                Text(
                    text = meta.transliteration + "  ·  " + typeLabel(meta.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun BismillahHeader() {
    Text(
        text = BISMILLAH,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = BrandGreen,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    )
}

/** Groups a surah's ayahs into consecutive Mushaf pages, preserving order. */
private fun groupByPage(ayahs: List<Ayah>): List<Pair<Int, List<Ayah>>> {
    val pages = mutableListOf<Pair<Int, MutableList<Ayah>>>()
    for (ayah in ayahs) {
        val last = pages.lastOrNull()
        if (last == null || last.first != ayah.page) {
            pages.add(ayah.page to mutableListOf(ayah))
        } else {
            last.second.add(ayah)
        }
    }
    return pages
}

private fun typeLabel(type: String): String = when (type.lowercase()) {
    "meccan" -> "مكية"
    "medinan" -> "مدنية"
    else -> type
}

private fun toArabicIndic(n: Int): String =
    n.toString().map { c -> if (c in '0'..'9') '٠' + (c - '0') else c }.joinToString("")
