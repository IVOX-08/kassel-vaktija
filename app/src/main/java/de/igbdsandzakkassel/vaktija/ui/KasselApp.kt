package de.igbdsandzakkassel.vaktija.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.igbdsandzakkassel.vaktija.ui.calendar.MonthCalendarScreen
import de.igbdsandzakkassel.vaktija.ui.dashboard.DashboardScreen
import de.igbdsandzakkassel.vaktija.ui.navigation.TopLevelDestination
import de.igbdsandzakkassel.vaktija.ui.news.NewsScreen
import de.igbdsandzakkassel.vaktija.ui.qibla.QiblaScreen
import de.igbdsandzakkassel.vaktija.ui.settings.SettingsScreen

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
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                        label = {
                            // Single line, smaller, ellipsised — keeps long German labels
                            // ("Einstellungen", "Nachrichten") from wrapping to two lines.
                            Text(
                                text = stringResource(destination.labelRes),
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        // Brand-green selection instead of the default purple.
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
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
            composable(TopLevelDestination.CALENDAR.route) { MonthCalendarScreen() }
            composable(TopLevelDestination.NEWS.route) { NewsScreen() }
            composable(TopLevelDestination.QIBLA.route) { QiblaScreen() }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
