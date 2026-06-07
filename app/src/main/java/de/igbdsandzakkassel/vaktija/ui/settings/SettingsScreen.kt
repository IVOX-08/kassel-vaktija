package de.igbdsandzakkassel.vaktija.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AlarmSettings
import de.igbdsandzakkassel.vaktija.data.settings.PrayerAlarmPrefs
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.ui.components.ChangeLanguagePill

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(stringResource(R.string.settings_theme_header))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(stringResource(mode.labelRes)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        SectionHeader(stringResource(R.string.settings_notifications_header))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_master_toggle),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = settings.masterEnabled,
                    onCheckedChange = viewModel::setMasterEnabled,
                )
            }
        }

        if (settings.masterEnabled) {
            Prayer.OBLIGATORY.forEach { prayer ->
                PrayerSettingCard(
                    prayer = prayer,
                    prefs = settings.prefs(prayer),
                    onDisable = { viewModel.setPrayerEnabled(prayer, false) },
                    onSelectMinutes = { viewModel.selectPreWarn(prayer, it) },
                )
            }
            Button(
                onClick = viewModel::testAdhan,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_test_adhan)) }
        }

        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_autosilence),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings.autoSilenceEnabled,
                        onCheckedChange = viewModel::setAutoSilence,
                    )
                }
                if (settings.autoSilenceEnabled) {
                    Text(
                        text = stringResource(R.string.settings_silence_duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlarmSettings.SILENCE_OPTIONS.forEach { minutes ->
                            FilterChip(
                                selected = settings.silenceMinutes == minutes,
                                onClick = { viewModel.setSilenceMinutes(minutes) },
                                label = { Text(stringResource(R.string.settings_minutes, minutes)) },
                            )
                        }
                    }
                    if (!viewModel.hasDndAccess()) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.settings_perm_dnd)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionHeader(stringResource(R.string.settings_permissions_header))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifPermission.status.isGranted) {
            OutlinedButton(
                onClick = { notifPermission.launchPermissionRequest() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_perm_notifications)) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !viewModel.canScheduleExact()) {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_perm_exact_alarm)) }
        }

        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.settings_perm_battery)) }

        Spacer(Modifier.height(8.dp))
        SectionHeader(stringResource(R.string.language_picker_title))
        ChangeLanguagePill()

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PrayerSettingCard(
    prayer: Prayer,
    prefs: PrayerAlarmPrefs,
    onDisable: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(prayer.labelRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            PreWarnSelector(prefs = prefs, onDisable = onDisable, onSelectMinutes = onSelectMinutes)
        }
    }
}

/**
 * Compact per-prayer alert selector: a pill showing the current choice that opens a dropdown — far
 * smaller than a row of chips. Off = no Adhan; 0 min = Adhan exactly on time; 5/10/15/30 min = Adhan
 * on time + a reminder that many minutes earlier.
 */
@Composable
private fun PreWarnSelector(
    prefs: PrayerAlarmPrefs,
    onDisable: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = if (!prefs.enabled) {
        stringResource(R.string.settings_off)
    } else {
        stringResource(R.string.settings_minutes, prefs.preWarnMinutes)
    }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(currentLabel)
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_off)) },
                onClick = {
                    onDisable()
                    expanded = false
                },
                trailingIcon = if (!prefs.enabled) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
            )
            AlarmSettings.PRE_WARN_OPTIONS.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_minutes, minutes)) },
                    onClick = {
                        onSelectMinutes(minutes)
                        expanded = false
                    },
                    trailingIcon = if (prefs.enabled && prefs.preWarnMinutes == minutes) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
