package de.igbdsandzakkassel.vaktija

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.core.device.isTelevision
import de.igbdsandzakkassel.vaktija.data.settings.ThemeMode
import de.igbdsandzakkassel.vaktija.ui.KasselApp
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
            KasselVaktijaTheme(darkTheme = darkTheme) {
                if (isTv) TvDashboardScreen() else KasselApp()
            }
        }
    }
}
