package de.igbdsandzakkassel.vaktija.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.igbdsandzakkassel.vaktija.ui.dashboard.DashboardScreen
import de.igbdsandzakkassel.vaktija.ui.navigation.TopLevelDestination
import de.igbdsandzakkassel.vaktija.ui.news.NewsScreen
import de.igbdsandzakkassel.vaktija.ui.qibla.QiblaScreen
import de.igbdsandzakkassel.vaktija.ui.settings.SettingsScreen
import de.igbdsandzakkassel.vaktija.ui.zakat.ZakatScreen

/**
 * Single-Activity phone/tablet scaffold: a bottom navigation bar over a Navigation-Compose host.
 * (TV builds use TvDashboardScreen directly and never reach this.)
 */
@Composable
fun KasselApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.DASHBOARD.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(TopLevelDestination.DASHBOARD.route) { DashboardScreen() }
            composable(TopLevelDestination.QIBLA.route) { QiblaScreen() }
            composable(TopLevelDestination.NEWS.route) { NewsScreen() }
            composable(TopLevelDestination.ZAKAT.route) { ZakatScreen() }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
