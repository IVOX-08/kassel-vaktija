package de.igbdsandzakkassel.vaktija.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
                    onEnabledChange = { viewModel.setPrayerEnabled(prayer, it) },
                    onPreWarnChange = { viewModel.setPreWarn(prayer, it) },
                )
            }
            Button(
                onClick = viewModel::testAdhan,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_test_adhan)) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerSettingCard(
    prayer: Prayer,
    prefs: PrayerAlarmPrefs,
    onEnabledChange: (Boolean) -> Unit,
    onPreWarnChange: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(prayer.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = prefs.enabled, onCheckedChange = onEnabledChange)
            }
            if (prefs.enabled) {
                Text(
                    text = stringResource(R.string.settings_prewarn),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlarmSettings.PRE_WARN_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = prefs.preWarnMinutes == minutes,
                            onClick = { onPreWarnChange(minutes) },
                            label = {
                                Text(
                                    if (minutes == 0) {
                                        stringResource(R.string.settings_off)
                                    } else {
                                        stringResource(R.string.settings_minutes, minutes)
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
