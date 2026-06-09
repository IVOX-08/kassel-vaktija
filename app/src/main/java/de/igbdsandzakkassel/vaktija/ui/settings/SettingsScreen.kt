package de.igbdsandzakkassel.vaktija.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.igbdsandzakkassel.vaktija.BuildConfig
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.settings.AdhanSound
import de.igbdsandzakkassel.vaktija.data.settings.AlarmSettings
import de.igbdsandzakkassel.vaktija.data.settings.PrayerAlarmPrefs
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.ui.components.ChangeLanguagePill
import java.time.format.DateTimeFormatter

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

    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val communityRules by viewModel.communityRules.collectAsStateWithLifecycle()
    var showLogin by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    val savedMsg = stringResource(R.string.admin_saved)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isAdmin) {
            SectionHeader(stringResource(R.string.admin_section_header))
            AdminSection(
                rules = communityRules,
                onSave = { edited ->
                    viewModel.saveRules(edited) {
                        Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                onSignOut = viewModel::signOut,
            )
            Spacer(Modifier.height(8.dp))
        }

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
            SoundSelectorCard(
                selected = settings.sound,
                onSelect = viewModel::setSound,
            )
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

        Spacer(Modifier.height(8.dp))
        SectionHeader(stringResource(R.string.settings_about_header))
        AboutCard(
            onVersionClick = {
                if (!isAdmin) {
                    versionTaps++
                    if (versionTaps >= 7) {
                        versionTaps = 0
                        showLogin = true
                    }
                }
            },
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showLogin) {
        AdminLoginDialog(
            onDismiss = { showLogin = false },
            onSignIn = { email, pw, cb -> viewModel.signIn(email, pw, cb) },
        )
    }
}

private const val COMMUNITY_EMAIL = "ikzsandzakkassel@gmail.com"
private const val PAYPAL_URL =
    "https://www.paypal.com/donate/?business=ikzsandzakkassel@gmail.com&currency_code=EUR"
private const val MAPS_URL =
    "https://www.google.com/maps/search/?api=1&query=Schwanenweg+13%2C+34123+Kassel"

private const val IMAM_NAME = "Alen Golac"
private const val IMAM_PHONE_DISPLAY = "0176 3037 2402"
private const val IMAM_PHONE_DIAL = "017630372402"
private const val DEV_PHONE_DISPLAY = "0176 6188 7123"
private const val DEV_PHONE_DIAL = "017661887123"

/** "About the community" card: name, address (→Maps), e-mail (→mail app), donate (→PayPal), version. */
@Composable
private fun AboutCard(onVersionClick: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.header_subtitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            AboutRow(Icons.Filled.Place, stringResource(R.string.community_address)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MAPS_URL)))
            }
            AboutRow(Icons.Filled.Email, COMMUNITY_EMAIL) {
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$COMMUNITY_EMAIL")))
            }
            AboutRow(Icons.Filled.Favorite, stringResource(R.string.action_donate)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL)))
            }
            AboutRow(
                Icons.Filled.Person,
                "${stringResource(R.string.about_imam)}: $IMAM_NAME\n$IMAM_PHONE_DISPLAY",
            ) {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$IMAM_PHONE_DIAL")))
            }
            AboutRow(
                Icons.Filled.Call,
                "${stringResource(R.string.about_dev_promo)}\n$DEV_PHONE_DISPLAY",
            ) {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$DEV_PHONE_DIAL")))
            }
            Spacer(Modifier.height(8.dp))
            // Tapping the version 7× reveals the hidden admin login.
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onVersionClick),
            )
        }
    }
}

/** Admin-only editor for the community-set times (synced to all users via Firestore). */
@Composable
private fun AdminSection(
    rules: CommunityRules,
    onSave: (CommunityRules) -> Unit,
    onSignOut: () -> Unit,
) {
    var draft by remember(rules) { mutableStateOf(rules) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StepperRow(
                label = "${stringResource(R.string.prayer_fajr)} ${stringResource(R.string.label_iqamah)}",
                value = draft.fajrIqamah.format(timeFmt),
                onMinus = { draft = draft.copy(fajrIqamah = draft.fajrIqamah.minusMinutes(5)) },
                onPlus = { draft = draft.copy(fajrIqamah = draft.fajrIqamah.plusMinutes(5)) },
            )
            StepperRow(
                label = stringResource(R.string.prayer_jumua),
                value = draft.jumua.format(timeFmt),
                onMinus = { draft = draft.copy(jumua = draft.jumua.minusMinutes(5)) },
                onPlus = { draft = draft.copy(jumua = draft.jumua.plusMinutes(5)) },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            OffsetRow(stringResource(R.string.prayer_dhuhr), draft.dhuhrOffsetMin) { draft = draft.copy(dhuhrOffsetMin = it) }
            OffsetRow(stringResource(R.string.prayer_asr), draft.asrOffsetMin) { draft = draft.copy(asrOffsetMin = it) }
            OffsetRow(stringResource(R.string.prayer_maghrib), draft.maghribOffsetMin) { draft = draft.copy(maghribOffsetMin = it) }
            OffsetRow(stringResource(R.string.prayer_isha), draft.ishaOffsetMin) { draft = draft.copy(ishaOffsetMin = it) }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(draft) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.admin_save))
                }
                OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.admin_sign_out)) }
            }
        }
    }
}

/** A prayer's Iqamah offset (minutes after Adhan), shown as "+N" with −/+ steppers (clamped 0..60). */
@Composable
private fun OffsetRow(label: String, value: Long, onChange: (Long) -> Unit) {
    StepperRow(
        label = label,
        value = "+$value",
        onMinus = { onChange((value - 1).coerceAtLeast(0)) },
        onPlus = { onChange((value + 1).coerceAtMost(60)) },
    )
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = "−") }
        Text(
            text = value,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(56.dp),
        )
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = "+") }
    }
}

@Composable
private fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onSignIn: (String, String, (Boolean) -> Unit) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_login_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.admin_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.admin_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (error) {
                    Text(
                        text = stringResource(R.string.admin_login_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    loading = true
                    error = false
                    onSignIn(email, password) { ok ->
                        loading = false
                        if (ok) onDismiss() else error = true
                    }
                },
            ) { Text(stringResource(R.string.admin_sign_in)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun AboutRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
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

/**
 * Notification-sound picker: a labelled row with a dropdown pill (full Adhan / short Adhan / chime /
 * vibrate only). Mirrors the per-prayer alert pill. "Test" (below) previews the current choice.
 */
@Composable
private fun SoundSelectorCard(
    selected: AdhanSound,
    onSelect: (AdhanSound) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_notification_sound),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Text(stringResource(selected.labelRes))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    AdhanSound.entries.forEach { sound ->
                        DropdownMenuItem(
                            text = { Text(stringResource(sound.labelRes)) },
                            onClick = {
                                onSelect(sound)
                                expanded = false
                            },
                            trailingIcon = if (sound == selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
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
