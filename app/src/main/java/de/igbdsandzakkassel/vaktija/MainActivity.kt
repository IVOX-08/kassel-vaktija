package de.igbdsandzakkassel.vaktija

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.core.device.isTelevision
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.ui.KasselApp
import de.igbdsandzakkassel.vaktija.ui.onboarding.OnboardingScreen
import de.igbdsandzakkassel.vaktija.ui.theme.KasselVaktijaTheme
import de.igbdsandzakkassel.vaktija.ui.tv.TvDashboardScreen

/**
 * The app's single Activity. Extends AppCompatActivity so per-app language selection
 * (AppCompatDelegate.setApplicationLocales) and RTL work reliably down to minSdk 26.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isTv = isTelevision()
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
            }
        }
    }
}
