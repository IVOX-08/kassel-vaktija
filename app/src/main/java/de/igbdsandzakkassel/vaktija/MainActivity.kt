package de.igbdsandzakkassel.vaktija

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.core.device.isTelevision
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.ui.KasselApp
import de.igbdsandzakkassel.vaktija.ui.onboarding.OnboardingScreen
import androidx.lifecycle.lifecycleScope
import de.igbdsandzakkassel.vaktija.service.widget.PrayerTimesWidgetReceiver
import de.igbdsandzakkassel.vaktija.ui.theme.KasselVaktijaTheme
import de.igbdsandzakkassel.vaktija.ui.tv.TvDashboardScreen
import kotlinx.coroutines.launch

/**
 * The app's single Activity. Extends AppCompatActivity so per-app language selection
 * (AppCompatDelegate.setApplicationLocales) and RTL work reliably down to minSdk 26.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // --- Google Play in-app updates: prompt the user when a newer version is on Play. ---
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private var pendingUpdate: AppUpdateInfo? = null
    private val updateAvailable = mutableStateOf(false) // "a new version is available" prompt
    private val updateDownloaded = mutableStateOf(false) // "downloaded — restart to install" prompt

    // A flexible update downloads in the background; this fires when it's ready to install.
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) updateDownloaded.value = true
    }

    // Must be registered unconditionally during creation (property initializer is fine).
    private val updateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // The download/install state is delivered via installListener, not this result.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isTv = isTelevision()
        // Wall-board TVs auto-update via the Play Store and have no one to tap a dialog; only
        // phones/tablets get the in-app update prompt.
        if (!isTv) checkForAppUpdate()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Status-bar & nav-bar icons must contrast the app background (driven by the app's own
            // theme, which can differ from the system theme via the in-app theme selector).
            // Light theme -> dark icons; dark theme -> light icons.
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
            val onboardingComplete by mainViewModel.onboardingComplete.collectAsStateWithLifecycle()
            KasselVaktijaTheme(darkTheme = darkTheme) {
                when {
                    isTv -> TvDashboardScreen()
                    onboardingComplete == false -> OnboardingScreen(onFinished = mainViewModel::completeOnboarding)
                    onboardingComplete == true -> KasselApp()
                    else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }

                if (updateAvailable.value) {
                    UpdateDialog(
                        title = stringResource(R.string.update_available_title),
                        body = stringResource(R.string.update_available_body),
                        confirm = stringResource(R.string.update_action),
                        onConfirm = {
                            updateAvailable.value = false
                            launchFlexibleUpdate()
                        },
                        onDismiss = { updateAvailable.value = false },
                    )
                }
                if (updateDownloaded.value) {
                    UpdateDialog(
                        title = stringResource(R.string.update_ready_title),
                        body = stringResource(R.string.update_ready_body),
                        confirm = stringResource(R.string.update_restart),
                        onConfirm = { appUpdateManager.completeUpdate() },
                        onDismiss = { updateDownloaded.value = false },
                    )
                }
            }
        }
    }

    /** Ask Play whether a newer version is available on the user's track; if so, raise the prompt. */
    private fun checkForAppUpdate() {
        appUpdateManager.registerListener(installListener)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            pendingUpdate = info
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> updateDownloaded.value = true
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> updateAvailable.value = true
            }
        }
    }

    private fun launchFlexibleUpdate() {
        val info = pendingUpdate ?: return
        runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isTelevision()) {
            // If a flexible update finished downloading while the app was in the background, prompt now.
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) updateDownloaded.value = true
            }
            // Snap the home-screen widget to the current next prayer right away. If an OEM battery
            // saver killed the widget's scheduled rollover while the app was closed (leaving it stuck
            // on a past prayer counting into the negative), opening the app instantly fixes it.
            lifecycleScope.launch { PrayerTimesWidgetReceiver.refresh(applicationContext) }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(installListener)
        super.onDestroy()
    }
}

@Composable
private fun UpdateDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
        },
    )
}
