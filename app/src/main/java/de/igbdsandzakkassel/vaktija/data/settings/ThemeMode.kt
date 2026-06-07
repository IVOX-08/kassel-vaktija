package de.igbdsandzakkassel.vaktija.data.settings

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R

/** User-selectable app theme. SYSTEM follows the device dark-mode setting. */
enum class ThemeMode(@param:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}
