package de.igbdsandzakkassel.vaktija.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldText
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.time.format.DateTimeFormatter

private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The prayer tracker.
 *
 * Each prayer can be answered only while its own time is running — from the Iqamah until the next
 * Adhan, and for Fajr only until sunrise. That is what makes the streak mean something: it cannot
 * be filled in at the end of the day from memory, and Fajr in particular has to be answered before
 * the sun is up.
 *
 * Everything stays on this phone.
 */
@Composable
fun PrayerTrackerScreen(modifier: Modifier = Modifier) {
    val viewModel: TrackerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StreakCard(streak = state.streak, prayedToday = state.prayedToday)

        Text(
            text = stringResource(R.string.tracker_today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )

        state.rows.forEach { row ->
            PrayerRow(
                row = row,
                onAnswer = { prayed -> viewModel.answer(row.prayer, prayed) },
            )
        }

        Text(
            text = stringResource(R.string.tracker_rule_explained),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun StreakCard(streak: Int, prayedToday: Int) {
    val reached = streak >= REWARD_DAYS
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reached) BrandGoldText.copy(alpha = 0.16f) else BrandGreen.copy(alpha = 0.12f),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (streak > 0) "🔥" else "🕌", fontSize = 40.sp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$streak",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (reached) BrandGoldText else BrandGreen,
                    )
                    Text(
                        text = stringResource(R.string.tracker_streak),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$prayedToday / 5",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(14.dp))
            // Progress toward the mosque's reward — the concrete reason the streak exists.
            LinearProgressIndicator(
                progress = { (streak.coerceAtMost(REWARD_DAYS).toFloat() / REWARD_DAYS) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (reached) BrandGoldText else BrandGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (reached) {
                    stringResource(R.string.tracker_reward_reached)
                } else {
                    stringResource(R.string.tracker_reward_progress, streak, REWARD_DAYS)
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                color = if (reached) BrandGoldText else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrayerRow(row: TrackerRow, onAnswer: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateIcon(row.state)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(row.prayer.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusLine(row),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (row.state) {
                            RowState.PRAYED -> BrandGreen
                            RowState.MISSED, RowState.NOT_PRAYED -> MaterialTheme.colorScheme.error
                            RowState.OPEN -> BrandGoldText
                            RowState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            // The two buttons exist only while the answer can still be given. An always-present
            // pair would invite ticking yesterday's prayers, which is the one thing this must not
            // allow.
            if (row.state == RowState.OPEN) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onAnswer(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_yes)) }
                    OutlinedButton(
                        onClick = { onAnswer(false) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_no)) }
                }
            }
        }
    }
}

@Composable
private fun StateIcon(state: RowState) {
    val (icon, tint) = when (state) {
        RowState.PRAYED -> Icons.Filled.CheckCircle to BrandGreen
        RowState.NOT_PRAYED, RowState.MISSED -> Icons.Filled.Cancel to MaterialTheme.colorScheme.error
        RowState.OPEN -> Icons.Outlined.Schedule to BrandGoldText
        RowState.UPCOMING -> Icons.Outlined.Lock to MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun statusLine(row: TrackerRow): String = when (row.state) {
    RowState.PRAYED -> stringResource(R.string.tracker_state_prayed)
    RowState.NOT_PRAYED -> stringResource(R.string.tracker_state_not_prayed)
    RowState.MISSED -> stringResource(R.string.tracker_state_missed)
    RowState.OPEN -> stringResource(R.string.tracker_state_open, row.closesAt.format(HM))
    RowState.UPCOMING -> stringResource(R.string.tracker_state_upcoming, row.opensAt.format(HM))
}

