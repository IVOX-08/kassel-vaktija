package de.igbdsandzakkassel.vaktija.service.notification

import android.content.Context
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.NewsRepository
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks Firestore on existing wake-ups (each prayer alarm, the daily refresh worker, app open) and
 * notifies the user — in their own app language — about two kinds of community change:
 *  - a newly-posted announcement (the `news` collection), and
 *  - an update to the prayer/Iqamah times (the `config/community` rules doc).
 *
 * This works on the free Firebase plan without Cloud Messaging; latency is until the next wake-up.
 * (Instant push would require FCM + a Cloud Function on the paid Blaze plan — that path posts the
 * same notifications, so it never double-notifies.)
 */
@Singleton
class NewsNotificationChecker @Inject constructor(
    private val newsRepository: NewsRepository,
    private val ruleProvider: CommunityRuleProvider,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun checkAndNotify(context: Context) {
        if (!settingsRepository.getNewsNotificationsEnabled()) return
        checkNews(context)
        checkConfig(context)
    }

    private suspend fun checkNews(context: Context) {
        val news = newsRepository.getLatestNews()?.takeIf { it.isNotEmpty() } ?: return
        val newest = news.first() // getLatestNews is sorted newest-first

        // createdAt is set by the ADMIN device's clock. Clamp the stored watermark to our OWN clock
        // so a future-dated post (admin clock skew) can never push the watermark past real time and
        // permanently suppress all later announcements.
        val watermark = minOf(newest.createdAt, System.currentTimeMillis())
        val lastNotified = settingsRepository.getLastNotifiedNewsMillis()

        if (lastNotified == null) {
            // First check ever: set the baseline so we don't notify the existing backlog.
            settingsRepository.setLastNotifiedNewsMillis(watermark)
            return
        }
        if (newest.createdAt <= lastNotified) return // nothing new since last time

        NewsNotifier.post(context, newest, currentLang(context))
        settingsRepository.setLastNotifiedNewsMillis(watermark)
    }

    private suspend fun checkConfig(context: Context) {
        val lastSeen = settingsRepository.getLastSeenConfigMillis()
        if (lastSeen == null) {
            // First check ever: baseline to NOW, so the NEXT real change (an admin save after this)
            // notifies — without notifying about times that were already set before this install.
            settingsRepository.setLastSeenConfigMillis(System.currentTimeMillis())
            return
        }
        val updatedAt = ruleProvider.getUpdatedAt() ?: return
        if (updatedAt <= lastSeen) return // times haven't changed since last time

        NewsNotifier.postConfigUpdate(context, currentLang(context))
        // Clamp the stored watermark to our OWN clock (like checkNews) so a future-dated edit (admin
        // clock skew) can never push it past real time and permanently suppress later changes.
        settingsRepository.setLastSeenConfigMillis(minOf(updatedAt, System.currentTimeMillis()))
    }

    // Prefer the tag persisted at selection time (reliable on a cold background wake, where
    // AppCompatDelegate.getApplicationLocales() — and thus LocaleController.current() — can read
    // empty and fall back to the default Bosnian). Then the DataStore tag, then the live value.
    private suspend fun currentLang(context: Context): String =
        LocaleController.persistedTag(context)
            ?: settingsRepository.getLanguageTag()
            ?: LocaleController.current().tag
}
