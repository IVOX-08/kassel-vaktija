package de.igbdsandzakkassel.vaktija.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = SurfaceLight,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGold,
    onSecondary = SurfaceLight,
    background = PageBackgroundLight,
    onBackground = NearBlack,
    surface = SurfaceLight,
    onSurface = NearBlack,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = NearBlack,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = BrandGreenDark,
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = OnDark,
    secondary = BrandGoldLight,
    onSecondary = NearBlack,
    background = BackgroundDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnDark,
)

/**
 * App theme. Material 3, dynamic color intentionally disabled so the community's
 * green/gold branding is consistent across devices.
 */
@Composable
fun KasselVaktijaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
