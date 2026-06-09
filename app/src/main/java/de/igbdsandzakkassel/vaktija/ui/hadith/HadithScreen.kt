package de.igbdsandzakkassel.vaktija.ui.hadith

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.hadith.HadithItem

/** Hadith collections picker: An-Nawawi's 40, and Riyad as-Salihin. */
@Composable
fun HadithCollectionsScreen(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CollectionCard(R.string.hadith_nawawi, subtitle = null) { onOpen("hadith_nawawi") }
        CollectionCard(R.string.hadith_riyad, subtitle = null) { onOpen("hadith_riyad") }
    }
}

@Composable
private fun CollectionCard(@StringRes titleRes: Int, subtitle: String?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Reader for one collection: each hadith as Arabic + the translation in the user's language. */
@Composable
fun HadithListScreen(
    collection: String,
    modifier: Modifier = Modifier,
    viewModel: HadithViewModel = hiltViewModel(),
) {
    val locales = LocalConfiguration.current.locales
    val lang = if (locales.isEmpty) LocaleController.current().tag else locales[0].language
    LaunchedEffect(collection, lang) { viewModel.load(collection, lang) }
    val items by viewModel.items.collectAsStateWithLifecycle()

    val list = items
    when {
        list == null -> Centered { CircularProgressIndicator() }
        list.isEmpty() -> Centered {
            Text(
                text = stringResource(R.string.placeholder_coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        else -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(list, key = { it.number }) { hadith -> HadithCard(hadith) }
            item { HadithCredit(creditFor(collection, lang)) }
        }
    }
}

/** Translator/source credit shown at the end of a collection (per collection + language). */
private fun creditFor(collection: String, lang: String): String =
    if (collection == "riyadussalihin") when (lang) {
        "bs" -> "Prijevod: Enciklopedija prevedenih hadisa (hadeethenc.com)."
        "tr" -> "Çeviri: Kandemir, Çakan, Küçük — Riyâzü's-Sâlihîn (Erkam)."
        "sq" -> "Përkthim: Rijadus Salihin (botim shqip)."
        "ru" -> "Перевод: Абдулла Нирша — „Сады праведных“ (Умма)."
        "ur" -> "ترجمہ: دائرۃ المعارف الحدیثیہ (hadeethenc.com)"
        "de" -> "Übersetzung: hadeethenc.com & dt. Ausgaben (Garten der Tugendhaften)."
        "en" -> "English: Riyad us-Saliheen (Darussalam, S. Yusuf)."
        else -> "المتن: رياض الصالحين للإمام النووي." // ar
    } else when (lang) { // nawawi40
        "bs" -> "Prijevod: Bilal Dervišić — „40 Nevevijevih hadisa“ (Sehara / minber.ba, 2010)."
        "sq" -> "Përktheu: Hoxhë Agim Bekiri (albislam.com)."
        "ur" -> "ترجمہ: شیخ عبد الہادی عبد الخالق مدنی"
        "ru" -> "Перевод: Абдулла (В.) Нирша."
        "de" -> "Übersetzung: Ahmad von Denffer & K. Richards (IIFSO)."
        "en" -> "English: Ezzeddin Ibrahim & Denys Johnson-Davies."
        else -> "Izvor / Source: hadith-api (public domain)." // ar, tr
    }

@Composable
private fun HadithCredit(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun HadithCard(hadith: HadithItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${hadith.number}.",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            // Arabic always renders right-to-left, regardless of the app's UI language.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = hadith.arabic,
                    fontSize = 21.sp,
                    lineHeight = 36.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (hadith.translation.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = hadith.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
