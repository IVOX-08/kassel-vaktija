package de.igbdsandzakkassel.vaktija.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Community
import java.text.Normalizer

/**
 * Choose which communities a federation announcement goes to.
 *
 * A regional event — a Hessen gathering, say — has no business buzzing phones in Rosenheim. The
 * default is still everyone, because that is the common case; this is for the times it is not.
 *
 * "Selecting every community" and "selecting none of them explicitly" are stored the same way, as
 * an empty list, so an announcement addressed to all keeps reaching communities that join later.
 */
@Composable
fun AudiencePickerDialog(
    communities: List<Community>,
    selected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val picked = remember { selected.toMutableStateList() }
    var query by remember { mutableStateOf("") }

    val shown = remember(query, communities) {
        val needle = query.fold()
        communities.filter { c ->
            needle.isBlank() ||
                c.name.fold().contains(needle) ||
                c.locations.any { it.name.fold().contains(needle) }
        }
    }
    val allPicked = picked.size == communities.size && communities.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.news_audience_title),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    if (allPicked) picked.clear()
                    else {
                        picked.clear()
                        picked.addAll(communities.map { it.id })
                    }
                }) {
                    Text(
                        stringResource(
                            if (allPicked) R.string.news_audience_none
                            else R.string.news_audience_all,
                        ),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(stringResource(R.string.community_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(shown, key = { it.id }) { community ->
                        val checked = community.id in picked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (checked) picked.remove(community.id)
                                    else picked.add(community.id)
                                }
                                .padding(end = 6.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(
                                    text = community.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = community.locations.joinToString(" · ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Everything selected is the same as no restriction — stored as an empty list so a
                // community that joins next month is included rather than quietly left out.
                onConfirm(if (allPicked) emptyList() else picked.toList())
            }) {
                Text(
                    if (picked.isEmpty() || allPicked) stringResource(R.string.news_audience_confirm_all)
                    else stringResource(R.string.news_audience_confirm, picked.size),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Accent-insensitive folding, same as the community picker: "munchen" finds "München". */
private fun String.fold(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .replace("đ", "d")
        .replace("ß", "ss")
