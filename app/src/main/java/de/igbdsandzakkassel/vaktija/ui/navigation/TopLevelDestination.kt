package de.igbdsandzakkassel.vaktija.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import de.igbdsandzakkassel.vaktija.R

/** Bottom-navigation destinations, in display order: Home, Month Calendar, News, Qibla, Settings. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Outlined.Home),
    CALENDAR("calendar", R.string.nav_calendar, Icons.Outlined.CalendarMonth),
    NEWS("news", R.string.nav_news, Icons.AutoMirrored.Outlined.Article),
    QIBLA("qibla", R.string.nav_qibla, Icons.Outlined.Explore),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings),
}
