package de.igbdsandzakkassel.vaktija.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import de.igbdsandzakkassel.vaktija.R

/**
 * The head admin's one field for the App Store link, shown on the day the iPhone app goes live.
 *
 * It exists as a field in the app rather than as a trip to the Firebase console for one reason: the
 * name of the field has to be exactly right, and a mistyped key in the console fails silently — the
 * boards would simply keep showing "coming soon" with nothing to explain why. Here there is nothing
 * to spell.
 *
 * What it changes is not this phone: it is the QR code on every wall board in every community, at
 * once, without any of those televisions fetching an app update. See
 * [de.igbdsandzakkassel.vaktija.data.store.StoreLinksRepository].
 */
@Composable
fun AppStoreLinkSection(
    currentLink: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(currentLink) }
    // The stored value arrives from Firestore a moment after the screen opens, and can also change
    // from another device. Follow it — but never while the admin is halfway through typing.
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(currentLink) {
        if (!touched) draft = currentLink
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.admin_ios_link_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    touched = true
                    // Pasted links carry stray spaces and line breaks often enough to be worth
                    // taking out here: one of them is the difference between a code that scans and
                    // a code that does not.
                    draft = it.trim()
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(stringResource(R.string.admin_ios_link_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        touched = false
                        onSave(draft)
                    },
                    enabled = draft != currentLink,
                ) { Text(stringResource(R.string.admin_save)) }
            }
        }
    }
}
