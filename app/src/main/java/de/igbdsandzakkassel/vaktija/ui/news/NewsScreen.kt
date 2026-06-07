package de.igbdsandzakkassel.vaktija.ui.news

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.components.PlaceholderContent

/** News / announcements — implemented in Phase 8 (admin compose UI in Phase 4b). */
@Composable
fun NewsScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(title = stringResource(R.string.nav_news), modifier = modifier)
}
