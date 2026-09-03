package de.igbdsandzakkassel.vaktija.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.ui.onboarding.InAppLanguagePicker

/** A language's flag, rendered as a small bordered rectangle. Decorative — the text labels it. */
@Composable
fun LanguageFlag(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    // Round, and Crop rather than Fit: a flag letterboxed inside a circle leaves grey wedges above
    // and below, which looks like a rendering fault rather than a design. Cropping the sides of a
    // 3:2 flag keeps the circle full and still shows the part people recognise.
    Image(
        painter = painterResource(language.flagRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}

/** Small pill button (current flag + label) that opens the in-app language picker. */
@Composable
fun ChangeLanguagePill(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<AppLanguage?>(null) }
    val current = LocaleController.current()
    SettingsNavRow(
        title = stringResource(current.displayNameRes),
        subtitle = stringResource(R.string.action_change_language),
        onClick = { show = true },
        modifier = modifier,
        leading = { LanguageFlag(current, size = 28.dp) },
    )
    if (show) {
        Dialog(
            onDismissRequest = { show = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            InAppLanguagePicker(
                onSelected = { chosen = it; show = false },
                onClose = { show = false },
            )
        }
    }
    // Apply the choice only AFTER the Dialog is gone. Calling setApplicationLocales from inside the
    // Dialog window failed to take effect on Android < 13; here it persists the tag + recreates.
    LaunchedEffect(chosen) {
        chosen?.let { LocaleController.set(context, it) }
    }
}
