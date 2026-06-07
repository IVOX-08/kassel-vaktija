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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController

/** A language's flag, rendered as a small bordered rectangle. Decorative — the text labels it. */
@Composable
fun LanguageFlag(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 24.dp,
) {
    val shape = RoundedCornerShape(3.dp)
    Image(
        painter = painterResource(language.flagRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    )
}

/**
 * Language selection dialog. Applying a choice calls [LocaleController.set], which persists the
 * selection and recreates the activity so the new locale + layout direction take effect.
 */
@Composable
fun LanguagePickerDialog(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(LocaleController.current()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_picker_title)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == language,
                                onClick = { selected = language },
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(
                            selected = selected == language,
                            onClick = { selected = language },
                        )
                        LanguageFlag(language)
                        Text(stringResource(language.displayNameRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                LocaleController.set(selected)
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** Small pill button (current flag + label) that opens the [LanguagePickerDialog]. */
@Composable
fun ChangeLanguagePill(modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    TextButton(onClick = { show = true }, modifier = modifier) {
        LanguageFlag(LocaleController.current(), width = 24.dp, height = 18.dp)
        Text(
            text = stringResource(R.string.action_change_language),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    if (show) LanguagePickerDialog(onDismiss = { show = false })
}
