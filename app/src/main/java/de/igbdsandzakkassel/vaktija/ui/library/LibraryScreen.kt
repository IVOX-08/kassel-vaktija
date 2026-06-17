package de.igbdsandzakkassel.vaktija.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.igbdsandzakkassel.vaktija.R

/** Items inside the "More" (Više) hub. Routes match the NavHost composables in KasselApp. */
enum class LibrarySection(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    QURAN("quran", R.string.library_quran, Icons.AutoMirrored.Outlined.MenuBook),
    HADITH("hadith", R.string.library_hadith, Icons.Outlined.FormatQuote),
    DHIKR("dhikr", R.string.library_dhikr, Icons.Outlined.SelfImprovement),
    TASBIH("tasbih", R.string.library_tasbih, Icons.Outlined.Adjust),
    TRACKER("tracker", R.string.library_tracker, Icons.Outlined.CheckCircle),
    RAMADAN("ramadan", R.string.library_ramadan, Icons.Outlined.DarkMode),
    QIBLA("qibla", R.string.nav_qibla, Icons.Outlined.Explore);

    companion object {
        /** Routes (hub + sub-pages) that should keep the "More" tab highlighted. */
        val ROUTES: Set<String> = setOf("library", "hadith_nawawi", "hadith_riyad", "quran_surah/{id}?ayah={ayah}") + entries.map { it.route }
    }
}

/** The "More" hub: a menu linking to Qur'an, Hadith, Dhikr and the Qibla compass. */
@Composable
fun LibraryScreen(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_more),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        LibrarySection.entries.forEach { section ->
            LibraryItem(section) { onOpen(section.route) }
        }
    }
}

@Composable
private fun LibraryItem(section: LibrarySection, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = stringResource(section.labelRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Wraps a hub sub-page with a back bar (the bottom navigation stays visible underneath). Used by the
 * Qur'an / Hadith / Dhikr / Qibla destinations.
 */
@Composable
fun LibraryDetail(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
