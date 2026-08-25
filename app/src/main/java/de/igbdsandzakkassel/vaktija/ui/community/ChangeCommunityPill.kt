package de.igbdsandzakkassel.vaktija.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.ui.components.SettingsNavRow
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen

/**
 * Settings row showing the current mosque; tapping it reopens the picker full-screen.
 *
 * Needed beyond onboarding for two reasons: people move, and a community that leaves the programme
 * is switched off — its members have to be able to move themselves somewhere else.
 */
@Composable
fun ChangeCommunityPill(
    modifier: Modifier = Modifier,
    viewModel: CurrentCommunityViewModel = hiltViewModel(),
) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    var pickerOpen by remember { mutableStateOf(false) }

    SettingsNavRow(
        title = selection?.location?.name.orEmpty(),
        subtitle = selection?.community?.name,
        onClick = { pickerOpen = true },
        modifier = modifier,
        leading = {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(22.dp),
            )
        },
    )

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
