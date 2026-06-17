package de.igbdsandzakkassel.vaktija.ui.tracker

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.time.LocalDate

private val PRAYERS = listOf(
    R.string.prayer_fajr to 0,
    R.string.prayer_dhuhr to 1,
    R.string.prayer_asr to 2,
    R.string.prayer_maghrib to 3,
    R.string.prayer_isha to 4,
)
private const val ALL_DONE = 0b11111 // all 5 prayers

/**
 * Private, on-device prayer tracker: check off the five daily prayers; a streak of consecutive
 * complete days gently motivates. Stored per day as a 5-bit mask in SharedPreferences — nothing
 * leaves the phone.
 */
@Composable
fun PrayerTrackerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now() }
    val todayKey = "d_$today"
    var mask by rememberSaveable { mutableIntStateOf(prefs.getInt(todayKey, 0)) }

    LaunchedEffect(mask) { prefs.edit().putInt(todayKey, mask).apply() }

    val doneCount = Integer.bitCount(mask)
    val streak = remember(mask) { computeStreak(prefs, today, mask) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Streak + today's progress.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("🔥", fontSize = 40.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$streak",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                    )
                    Text(
                        text = stringResource(R.string.tracker_streak),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$doneCount / 5",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (mask == ALL_DONE) BrandGold else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(R.string.tracker_today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )

        PRAYERS.forEach { (labelRes, bit) ->
            val done = mask and (1 shl bit) != 0
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mask = mask xor (1 shl bit) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) BrandGreen else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Consecutive days (ending today, or yesterday if today is not yet complete) with all 5 prayers. */
private fun computeStreak(prefs: SharedPreferences, today: LocalDate, todayMask: Int): Int {
    var streak = 0
    var day = if (todayMask == ALL_DONE) today else today.minusDays(1)
    while (prefs.getInt("d_$day", 0) == ALL_DONE) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}
