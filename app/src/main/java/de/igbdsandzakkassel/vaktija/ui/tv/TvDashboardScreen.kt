package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.igbdsandzakkassel.vaktija.R

/**
 * Android TV / Fire TV variant of the Dashboard: no bottom navigation, no Qibla/Zakat/News/
 * Settings, a big clock and a clean prayer-times table. Notifications and DND are disabled on
 * TV builds. The full table arrives with the data layer.
 *
 * Phase 0 shows a TV-styled placeholder so leanback routing can be verified.
 */
@Composable
fun TvDashboardScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_community),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(280.dp),
                )
                // TBD-asset: big live clock + prayer-times table land with Phase 1/2 data.
                Text(
                    text = stringResource(R.string.placeholder_coming_soon),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
