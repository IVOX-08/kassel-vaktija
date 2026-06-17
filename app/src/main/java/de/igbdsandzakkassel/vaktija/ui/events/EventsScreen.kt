package de.igbdsandzakkassel.vaktija.ui.events

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import de.igbdsandzakkassel.vaktija.data.model.EventItem
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Community events feed (lectures, Mevlud, mektep, Bajram times). Admin posts/deletes; all read. */
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val locales = LocalConfiguration.current.locales
    val lang = if (locales.isEmpty) LocaleController.current().tag else locales[0].language

    val context = LocalContext.current
    val partialMsg = stringResource(R.string.news_translate_partial)
    val failMsg = stringResource(R.string.news_post_failed)

    var showCompose by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<EventItem?>(null) }
    val todayStart = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        if (isAdmin) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showCompose = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.events_add))
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }

        val list = events
        when {
            list == null -> CenteredBox { CircularProgressIndicator() }
            else -> {
                val upcoming = list.filter { it.eventAt >= todayStart }
                if (upcoming.isEmpty()) {
                    CenteredBox {
                        Text(
                            text = stringResource(R.string.events_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(upcoming, key = { it.id }) { item ->
                            EventCard(item, lang, isAdmin) { pendingDelete = item }
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    if (showCompose) {
        ComposeEventDialog(
            onDismiss = { showCompose = false },
            onPost = { title, body, eventAt, cb ->
                viewModel.postEvent(title, body, eventAt) { outcome ->
                    when {
                        !outcome.ok -> Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
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
                    viewModel.deleteEvent(target.id)
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
private fun EventCard(item: EventItem, lang: String, canDelete: Boolean, onDelete: () -> Unit) {
    val body = item.body(lang)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatEvent(item.eventAt),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
            )
            Spacer(Modifier.height(4.dp))
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
            if (body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeEventDialog(
    onDismiss: () -> Unit,
    onPost: (String, String, Long, (Boolean) -> Unit) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    // Default: today at 19:00 (a typical event time).
    var eventAt by remember {
        mutableLongStateOf(LocalDate.now().atTime(19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.events_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 200) title = it },
                    label = { Text(stringResource(R.string.news_title_label)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 4000) body = it },
                    label = { Text(stringResource(R.string.news_body_label)) },
                    minLines = 2,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = formatEvent(eventAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDate = true }, enabled = !loading) {
                        Text(stringResource(R.string.events_date))
                    }
                    OutlinedButton(onClick = { showTime = true }, enabled = !loading) {
                        Text(stringResource(R.string.events_time))
                    }
                }
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
                    onPost(title, body, eventAt) { loading = false }
                },
            ) { Text(stringResource(R.string.news_post)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )

    if (showDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = eventAt)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { eventAt = mergeDate(it, eventAt) }
                    showDate = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showTime) {
        val current = Instant.ofEpochMilli(eventAt).atZone(ZoneId.systemDefault())
        val timeState = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    eventAt = mergeTime(eventAt, timeState.hour, timeState.minute)
                    showTime = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text(stringResource(android.R.string.cancel)) }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

/** Replace the date part of [base] with the date in [dateMillisUtc] (which is UTC midnight). */
private fun mergeDate(dateMillisUtc: Long, base: Long): Long {
    val date = Instant.ofEpochMilli(dateMillisUtc).atZone(ZoneOffset.UTC).toLocalDate()
    val time = Instant.ofEpochMilli(base).atZone(ZoneId.systemDefault()).toLocalTime()
    return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** Replace the time part of [base] with [hour]:[minute]. */
private fun mergeTime(base: Long, hour: Int, minute: Int): Long {
    val date = Instant.ofEpochMilli(base).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatEvent(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, d. MMM yyyy · HH:mm", Locale.getDefault()))
