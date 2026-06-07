package de.igbdsandzakkassel.vaktija.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import de.igbdsandzakkassel.vaktija.R

/** The five bottom-navigation destinations (phone/tablet builds). */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Outlined.Home),
    QIBLA("qibla", R.string.nav_qibla, Icons.Outlined.Explore),
    NEWS("news", R.string.nav_news, Icons.AutoMirrored.Outlined.Article),
    ZAKAT("zakat", R.string.nav_zakat, Icons.Outlined.Calculate),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings),
}
