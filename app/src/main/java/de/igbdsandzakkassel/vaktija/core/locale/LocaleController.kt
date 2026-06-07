package de.igbdsandzakkassel.vaktija.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Thin wrapper over the AndroidX per-app language API. Selection is persisted automatically by
 * the AppLocalesMetadataHolderService declared in the manifest (autoStoreLocales=true), and
 * setting it recreates the activity so the new locale (and RTL direction) takes effect.
 */
object LocaleController {

    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) null else locales[0]?.language
        return AppLanguage.fromTag(tag)
    }

    fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag),
        )
    }
}
