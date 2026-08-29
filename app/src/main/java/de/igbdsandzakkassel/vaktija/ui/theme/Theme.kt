package de.igbdsandzakkassel.vaktija.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Every surface role is set, not only the obvious ones.
 *
 * Material fills whatever is left out with its own baseline palette, and that baseline is purple.
 * `Card` does not draw on `surface` — it draws on `surfaceContainerLow`, which nobody had set, so
 * every card in the app carried a lavender cast: three cards on the settings screen in three
 * different tints, and a mauve wash behind the prayer tracker. The fix is to leave nothing to the
 * baseline.
 */
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = SurfaceLight,
    primaryContainer = BrandGreenContainer,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGold,
    onSecondary = SurfaceLight,
    secondaryContainer = BrandGoldContainer,
    onSecondaryContainer = BrandGoldDeep,
    tertiary = BrandGold,
    onTertiary = SurfaceLight,
    tertiaryContainer = BrandGoldContainer,
    onTertiaryContainer = BrandGoldDeep,

    background = PageBackgroundLight,
    onBackground = NearBlack,
    surface = SurfaceLight,
    onSurface = NearBlack,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceMutedLight,

    // Cards, sheets and menus. White for a card on a light-grey page, the way a printed card sits
    // on paper — and the way the iPhone version does it.
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceVariantLight,
    surfaceContainerHighest = SurfaceContainerHighLight,
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceTint = BrandGreen,

    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = NearBlack,
    inverseOnSurface = SurfaceLight,
    inversePrimary = BrandGreenLight,
    scrim = ScrimColor,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = BrandGreenDark,
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = OnDark,
    secondary = BrandGoldLight,
    onSecondary = NearBlack,
    secondaryContainer = BrandGoldDeep,
    onSecondaryContainer = BrandGoldLight,
    tertiary = BrandGoldLight,
    onTertiary = NearBlack,
    tertiaryContainer = BrandGoldDeep,
    onTertiaryContainer = BrandGoldLight,

    background = BackgroundDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMutedDark,

    // Stepped, so a card on a card is still distinguishable without any of them going purple.
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceBright = SurfaceContainerHighDark,
    surfaceDim = BackgroundDark,
    surfaceTint = BrandGreenLight,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = OnDark,
    inverseOnSurface = NearBlack,
    inversePrimary = BrandGreen,
    scrim = ScrimColor,
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
