package de.igbdsandzakkassel.vaktija.core.locale

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R

/**
 * Languages the app ships. Keep in sync with res/values-XX/ and res/xml/locales_config.xml.
 * The architecture does not assume a fixed count — adding an entry here plus a values-XX/ folder
 * and a <locale> line in locales_config.xml is all that is required.
 *
 * Each language carries a flag drawable (real images, since Android's system font does not render
 * flag emoji). Arabic uses the Palestinian flag by community choice.
 */
enum class AppLanguage(
    val tag: String,
    @param:StringRes val displayNameRes: Int,
    @param:DrawableRes val flagRes: Int,
) {
    BOSNIAN("bs", R.string.lang_bs, R.drawable.flag_ba),
    GERMAN("de", R.string.lang_de, R.drawable.flag_de),
    ARABIC("ar", R.string.lang_ar, R.drawable.flag_ps),
    TURKISH("tr", R.string.lang_tr, R.drawable.flag_tr),
    ALBANIAN("sq", R.string.lang_sq, R.drawable.flag_al),
    ENGLISH("en", R.string.lang_en, R.drawable.flag_gb),
    URDU("ur", R.string.lang_ur, R.drawable.flag_pk),
    RUSSIAN("ru", R.string.lang_ru, R.drawable.flag_ru);

    companion object {
        /** Primary/default language. */
        val DEFAULT = BOSNIAN

        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag != null && tag.startsWith(it.tag) } ?: DEFAULT
    }
}
