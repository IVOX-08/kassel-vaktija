package de.igbdsandzakkassel.vaktija.ui.settings

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
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.data.model.CommunityStatus
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldText
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.text.Normalizer

/**
 * The head admin's list of every community and the state it is in.
 *
 * This is the lever the whole arrangement rests on: a community that stops paying is suspended and
 * loses its presence while keeping its prayer times, and one disrupting the programme is blocked
 * outright. Both are reversible from the same place — the ordinary case is a community that pays
 * late, not one that leaves.
 */
@Composable
fun CommunityAdminSection(
    communities: List<Community>,
    onSetStatus: (String, CommunityStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Community?>(null) }

    val shown = remember(query, communities) {
        val needle = query.fold()
        communities
            .filter {
                needle.isBlank() || it.name.fold().contains(needle) ||
                    it.locations.any { l -> l.name.fold().contains(needle) }
            }
            // Anything switched off sorts first — those are the ones that need looking at.
            .sortedWith(compareBy({ it.status == CommunityStatus.ACTIVE }, { it.name.lowercase() }))
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(shown, key = { it.id }) { community ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { editing = community }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        StatusChip(community.status)
                    }
                }
            }
        }
    }

    editing?.let { community ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(community.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommunityStatus.entries.forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (status == community.status) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                )
                                .clickable {
                                    onSetStatus(community.id, status)
                                    editing = null
                                }
                                .padding(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(status.labelRes()),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(status.explanationRes()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusChip(status: CommunityStatus) {
    val color = when (status) {
        CommunityStatus.ACTIVE -> BrandGreen
        CommunityStatus.SUSPENDED -> BrandGoldText
        CommunityStatus.BLOCKED -> MaterialTheme.colorScheme.error
    }
    Text(
        text = stringResource(status.labelRes()),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private fun CommunityStatus.labelRes() = when (this) {
    CommunityStatus.ACTIVE -> R.string.admin_status_active
    CommunityStatus.SUSPENDED -> R.string.admin_status_suspended
    CommunityStatus.BLOCKED -> R.string.admin_status_blocked
}

private fun CommunityStatus.explanationRes() = when (this) {
    CommunityStatus.ACTIVE -> R.string.admin_status_active_hint
    CommunityStatus.SUSPENDED -> R.string.admin_status_suspended_hint
    CommunityStatus.BLOCKED -> R.string.admin_status_blocked_hint
}

/** Accent-insensitive folding, same as the pickers: "munchen" finds "München". */
private fun String.fold(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .replace("đ", "d")
        .replace("ß", "ss")
