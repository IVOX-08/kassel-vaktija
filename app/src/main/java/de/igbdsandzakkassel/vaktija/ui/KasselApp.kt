package de.igbdsandzakkassel.vaktija.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.dhikr.DhikrScreen
import de.igbdsandzakkassel.vaktija.ui.hadith.HadithCollectionsScreen
import de.igbdsandzakkassel.vaktija.ui.hadith.HadithListScreen
import de.igbdsandzakkassel.vaktija.ui.library.LibraryDetail
import de.igbdsandzakkassel.vaktija.ui.library.LibraryScreen
import de.igbdsandzakkassel.vaktija.ui.library.LibrarySection
import de.igbdsandzakkassel.vaktija.ui.quran.QuranListScreen
import de.igbdsandzakkassel.vaktija.ui.quran.QuranSurahScreen
import de.igbdsandzakkassel.vaktija.ui.tasbih.TasbihScreen

/**
 * Single-Activity phone/tablet scaffold: a bottom navigation bar over a Navigation-Compose host.
 * (TV builds use TvDashboardScreen directly and never reach this.)
 */
@Composable
fun KasselApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    // Hide the bottom bar while scrolling content down, show it again on scroll up.
    var barVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y < -1f) barVisible = false
                else if (consumed.y > 1f) barVisible = true
                return Offset.Zero
            }
        }
    }
    // Always reveal the bar when switching tabs.
    LaunchedEffect(currentRoute?.route) { barVisible = true }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            AnimatedVisibility(
                visible = barVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = if (destination == TopLevelDestination.LIBRARY) {
                        currentRoute?.route in LibrarySection.ROUTES
                    } else {
                        currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    }
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
                                fontSize = 9.sp,
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
            composable(TopLevelDestination.LIBRARY.route) {
                LibraryScreen(onOpen = { route -> navController.navigate(route) })
            }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }

            // "More" (Više) hub sub-pages. Bottom bar stays visible; back returns to the hub.
            composable(LibrarySection.QIBLA.route) {
                LibraryDetail(R.string.nav_qibla, onBack = { navController.popBackStack() }) { QiblaScreen() }
            }
            composable(LibrarySection.DHIKR.route) {
                LibraryDetail(R.string.library_dhikr, onBack = { navController.popBackStack() }) { DhikrScreen() }
            }
            composable(LibrarySection.TASBIH.route) {
                LibraryDetail(R.string.library_tasbih, onBack = { navController.popBackStack() }) { TasbihScreen() }
            }
            composable(LibrarySection.QURAN.route) {
                LibraryDetail(R.string.library_quran, onBack = { navController.popBackStack() }) {
                    QuranListScreen(onOpen = { id -> navController.navigate("quran_surah/$id") })
                }
            }
            composable(
                route = "quran_surah/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                QuranSurahScreen(
                    surahId = entry.arguments?.getInt("id") ?: 1,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(LibrarySection.HADITH.route) {
                LibraryDetail(R.string.library_hadith, onBack = { navController.popBackStack() }) {
                    HadithCollectionsScreen(onOpen = { route -> navController.navigate(route) })
                }
            }
            composable("hadith_nawawi") {
                LibraryDetail(R.string.hadith_nawawi, onBack = { navController.popBackStack() }) {
                    HadithListScreen(collection = "nawawi40")
                }
            }
            composable("hadith_riyad") {
                LibraryDetail(R.string.hadith_riyad, onBack = { navController.popBackStack() }) {
                    HadithListScreen(collection = "riyadussalihin")
                }
            }
        }
    }
}
