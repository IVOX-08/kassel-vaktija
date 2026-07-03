package de.igbdsandzakkassel.vaktija.ui.tasbih

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen

/**
 * Digital Tasbih (dhikr counter): tap the big circle to count. A target of 33 / 99 (or ∞) fills the
 * ring and buzzes when a round completes; the count, rounds and target persist across visits. A light
 * tick on every tap gives the tactile feel of prayer beads.
 */
@Composable
fun TasbihScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tasbih_prefs", Context.MODE_PRIVATE) }
    var target by rememberSaveable { mutableIntStateOf(prefs.getInt("target", 33)) }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("count", 0)) }

    LaunchedEffect(count, target) {
        prefs.edit().putInt("count", count).putInt("target", target).apply()
    }

    val inRound = if (target > 0) count % target else count
    val rounds = if (target > 0) count / target else 0
    val progress = if (target > 0) inRound.toFloat() / target else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(33, 99, 0).forEach { t ->
                FilterChip(
                    selected = target == t,
                    onClick = { target = t },
                    label = { Text(if (t == 0) "∞" else t.toString()) },
                )
            }
        }

        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(BrandGreen.copy(alpha = 0.08f))
                .clickable {
                    count++
                    if (target > 0 && count % target == 0) vibrate(context, 200L) else vibrate(context, 16L)
                },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(260.dp),
                strokeWidth = 10.dp,
                color = BrandGreen,
                trackColor = BrandGreen.copy(alpha = 0.15f),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                )
                if (target > 0) {
                    Text(
                        text = "$inRound / $target",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.tasbih_rounds, rounds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.tasbih_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Confirm before wiping: the reset button sits right under the rhythmic tap area — a
        // fumbled tap 250 counts into a session must not silently destroy the progress.
        var confirmReset by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { if (count > 0) confirmReset = true }) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tasbih_reset))
        }
        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text(stringResource(R.string.tasbih_reset)) },
                text = { Text(stringResource(R.string.tasbih_reset_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        count = 0
                        confirmReset = false
                    }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmReset = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

private fun vibrate(context: Context, ms: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    runCatching { vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
}
