package de.igbdsandzakkassel.vaktija.core.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Thin wrapper over the AndroidX per-app language API. Selecting a language recreates the Activity so
 * the new locale (and RTL direction) takes effect, and — via [set] with a Context — the chosen tag is
 * also written to a tiny SharedPreferences so background workers can localize notifications reliably.
 *
 * Why the extra persistence: on a cold background wake (a worker process with no Activity), and for a
 * short window right after a locale-change Activity recreate, `AppCompatDelegate.getApplicationLocales()`
 * can read EMPTY — which would otherwise make notifications fall back to the default (Bosnian). The
 * SharedPreferences tag, written at the exact moment of selection, is the reliable source of truth.
 */
object LocaleController {

    private const val PREFS = "locale_prefs"
    private const val KEY_TAG = "app_lang_tag"

    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) null else locales[0]?.language
        return AppLanguage.fromTag(tag)
    }

    /** Legacy apply (no persistence) — kept for callers without a Context. Prefer [set] with a Context. */
    fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    /** Persist the chosen tag (reliable for background localization) and apply it (recreates the Activity). */
    fun set(context: Context, language: AppLanguage) {
        persist(context, language.tag)
        set(language)
    }

    /** Persist just the language tag — e.g. from onResume, to back-fill installs updated from an older build. */
    fun persist(context: Context, tag: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .apply()
    }

    /**
     * The language to use for anything built OUTSIDE an Activity — a notification, a widget.
     *
     * Ask this, never [current], anywhere a background wake-up is possible. [current] falls back to
     * [AppLanguage.DEFAULT] (Bosnian) when `AppCompatDelegate` reads empty, and it reads empty
     * exactly in the situations background code runs in. A fallback is the right answer for a
     * screen, which has to render something; it is the wrong answer for a stored setting, because
     * it is indistinguishable from a real choice once written down.
     *
     * That is not hypothetical: the tracker asked "Jesi li klanjao Akšam?" of a user who had set
     * the app to German. The guessed tag had been written over his real one.
     *
     * Returns null only when the app has genuinely never been told a language.
     */
    fun resolvedTag(context: Context): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) return locales[0]?.language
        return persistedTag(context)
    }

    /** The last selected language tag, or null if none was ever persisted. */
    fun persistedTag(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
}
