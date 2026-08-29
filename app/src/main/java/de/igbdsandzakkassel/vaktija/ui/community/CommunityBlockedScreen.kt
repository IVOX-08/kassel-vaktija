package de.igbdsandzakkassel.vaktija.ui.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.igbdsandzakkassel.vaktija.R

/**
 * Shown instead of the app when the selected community has been blocked.
 *
 * A blocked community keeps nothing — not even prayer times — so there is no half-working screen to
 * fall back to. What the screen must do instead is give the person standing in front of it a way
 * forward, hence the picker button: they are almost certainly not the reason their community was
 * blocked.
 */
@Composable
fun CommunityBlockedScreen(modifier: Modifier = Modifier) {
    var pickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_igbd),
            contentDescription = null,
            modifier = Modifier.height(96.dp),
        )
        Text(
            text = stringResource(R.string.community_blocked_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.community_blocked_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        Button(
            onClick = { pickerOpen = true },
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        ) { Text(stringResource(R.string.community_blocked_action)) }
    }

    if (pickerOpen) {
        Dialog(
            onDismissRequest = { pickerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            CommunityPickerScreen(
                onSelected = { pickerOpen = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
