package de.igbdsandzakkassel.vaktija.ui.onboarding

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen

/**
 * The onboarding steps that follow the intro: a friendly, ONE-question-at-a-time wizard that asks for
 * what the app needs, in plain terms —
 *   1. notifications (so prayer/Adhan reminders and community announcements arrive),
 *   2. Do-Not-Disturb access (so the Adhan can still sound when the phone is silenced),
 *   3. the home-screen widget (offered with a one-tap "pin to home screen").
 *
 * Everything is optional and skippable, and can be (re)done later in Settings. The exact-alarm and
 * battery-exemption toggles deliberately live ONLY in Settings to keep this first-run flow simple.
 */
@Composable
fun OnboardingPermissions(onFinished: () -> Unit) {
    val context = LocalContext.current

    // Survives the configuration changes MainActivity handles itself; the wizard keeps its place.
    var step by rememberSaveable { mutableIntStateOf(0) }
    var widgetRequested by rememberSaveable { mutableStateOf(false) }
    // Whether we've already fired the system notification-permission dialog once. After that, if it's
    // still not granted the OS won't show the dialog again (permanent denial), so we send the user to
    // app settings instead of re-launching a request that would silently do nothing.
    var notifRequested by rememberSaveable { mutableStateOf(false) }

    // Re-read the live permission statuses whenever we come back from a system settings screen.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifRequested = true; refresh++ }

    val pinSupported = remember {
        context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true
    }

    refresh // read so the statuses below recompute after a settings/permission round-trip
    val notifGranted = notificationsAllowed(context)
    val dndGranted = dndAccessAllowed(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Same edge-to-edge trap as the intro: without this the step dots sit under the status
            // bar and the buttons under the gesture bar.
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepDots(current = step, total = TOTAL_STEPS)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                0 -> StepBody(
                    icon = Icons.Outlined.Notifications,
                    titleRes = R.string.onb_perm_notif_title,
                    bodyRes = R.string.onb_perm_notif_body,
                    granted = notifGranted,
                )
                1 -> StepBody(
                    icon = Icons.Outlined.DoNotDisturbOn,
                    titleRes = R.string.onb_perm_dnd_title,
                    bodyRes = R.string.onb_perm_dnd_body,
                    granted = dndGranted,
                )
                else -> StepBody(
                    icon = Icons.Outlined.Widgets,
                    titleRes = R.string.onb_perm_widget_title,
                    bodyRes = R.string.onb_perm_widget_body,
                    granted = null,
                    noteRes = if (!pinSupported) R.string.onb_perm_widget_unsupported else null,
                )
            }
        }

        when (step) {
            0 -> PermActions(
                granted = notifGranted,
                actionLabelRes = R.string.settings_perm_notifications,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifRequested) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // <33, or the system dialog was already shown once and denied (it won't
                        // reappear) → open app notification settings so the tap always does something.
                        openAppNotificationSettings(context)
                    }
                },
                onNext = { step = 1 },
            )
            1 -> PermActions(
                granted = dndGranted,
                actionLabelRes = R.string.settings_perm_dnd,
                onAction = { openDndAccess(context) },
                onNext = { step = 2 },
            )
            else -> WidgetActions(
                canPin = pinSupported && !widgetRequested,
                onAdd = {
                    requestPinWidget(context)
                    widgetRequested = true
                },
                onFinish = onFinished,
            )
        }
    }
}

/** Three-dot progress indicator, mirroring the intro pager's dots. */
@Composable
private fun StepDots(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        repeat(total) { i ->
            val width by animateDpAsState(if (current == i) 22.dp else 8.dp, label = "stepdot")
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (current == i) BrandGreen else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun StepBody(
    icon: ImageVector,
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    granted: Boolean?,
    @StringRes noteRes: Int? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandGreen,
            modifier = Modifier.size(88.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(titleRes),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (granted == true) {
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(22.dp))
                Text(
                    text = stringResource(R.string.settings_perm_granted),
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandGreen,
                )
            }
        }
        if (noteRes != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(noteRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Bottom buttons for a permission step: grant (or, once granted, continue) + skip. */
@Composable
private fun PermActions(
    granted: Boolean,
    @StringRes actionLabelRes: Int,
    onAction: () -> Unit,
    onNext: () -> Unit,
) {
    if (granted) {
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_next))
        }
    } else {
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(actionLabelRes))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_skip))
        }
    }
}

/** Bottom buttons for the final widget step: offer the one-tap pin, then finish. */
@Composable
private fun WidgetActions(canPin: Boolean, onAdd: () -> Unit, onFinish: () -> Unit) {
    if (canPin) {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_perm_widget_action))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_skip))
        }
    } else {
        // Already requested, or the launcher can't pin programmatically → just finish.
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_finish))
        }
    }
}

// --- Permission status checks ---

private fun notificationsAllowed(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

private fun dndAccessAllowed(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted ?: false

// --- Actions ---

private fun openAppNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun openDndAccess(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Ask the launcher to pin the prayer-times widget to the home screen (shows the system confirm). */
private fun requestPinWidget(context: Context) {
    val manager = context.getSystemService(AppWidgetManager::class.java) ?: return
    if (!manager.isRequestPinAppWidgetSupported) return
    runCatching {
        manager.requestPinAppWidget(
            ComponentName(context, PrayerTimesWidgetReceiver::class.java),
            null,
            null,
        )
    }
}

private const val TOTAL_STEPS = 3
