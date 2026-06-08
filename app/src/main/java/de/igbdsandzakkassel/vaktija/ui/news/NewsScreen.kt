package de.igbdsandzakkassel.vaktija.ui.news

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Community announcements feed. Admin (when signed in) can post and delete; everyone else reads. */
@Composable
fun NewsScreen(
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val news by viewModel.news.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    // The user's selected app language — each announcement is shown in this language.
    val locales = LocalConfiguration.current.locales
    val lang = if (locales.isEmpty) LocaleController.current().tag else locales[0].language

    val context = LocalContext.current
    val partialMsg = stringResource(R.string.news_translate_partial)
    val failMsg = stringResource(R.string.news_post_failed)

    var showCompose by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<NewsItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_news),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        if (isAdmin) {
            Button(
                onClick = { showCompose = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.news_add))
            }
            Spacer(Modifier.height(12.dp))
        }

        val list = news
        when {
            list == null -> CenteredBox { CircularProgressIndicator() }
            list.isEmpty() -> CenteredBox {
                Text(
                    text = stringResource(R.string.news_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { item ->
                    NewsCard(
                        item = item,
                        lang = lang,
                        canDelete = isAdmin,
                        onDelete = { pendingDelete = item },
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (showCompose) {
        ComposeNewsDialog(
            onDismiss = { showCompose = false },
            onPost = { title, body, cb ->
                viewModel.postNews(title, body) { outcome ->
                    when {
                        !outcome.ok ->
                            Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
                        outcome.failedLangs.isNotEmpty() ->
                            Toast.makeText(context, partialMsg, Toast.LENGTH_LONG).show()
                    }
                    cb(outcome.ok)
                    if (outcome.ok) showCompose = false
                }
            },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.news_delete_confirm)) },
            text = { Text(target.title(lang)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNews(target.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.news_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun NewsCard(item: NewsItem, lang: String, canDelete: Boolean, onDelete: () -> Unit) {
    val body = item.body(lang)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.news_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (item.createdAt > 0L) {
                Text(
                    text = formatTimestamp(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (body.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ComposeNewsDialog(
    onDismiss: () -> Unit,
    onPost: (String, String, (Boolean) -> Unit) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    AlertDialog(
        // Block dismissal (back press / outside tap) while translating + posting is in flight.
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.news_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= TITLE_MAX_CHARS) title = it },
                    label = { Text(stringResource(R.string.news_title_label)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= BODY_MAX_CHARS) body = it },
                    label = { Text(stringResource(R.string.news_body_label)) },
                    minLines = 3,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.news_translating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && title.isNotBlank(),
                onClick = {
                    loading = true
                    onPost(title, body) { loading = false }
                },
            ) { Text(stringResource(R.string.news_post)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

// Keep announcements well within ML Kit's translate limits and Firestore's 1 MB document size
// (8 language copies × title+body).
private const val TITLE_MAX_CHARS = 200
private const val BODY_MAX_CHARS = 4000

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d. MMMM yyyy, HH:mm", Locale.getDefault()))
