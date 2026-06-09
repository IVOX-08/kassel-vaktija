package de.igbdsandzakkassel.vaktija.service.notification

import android.content.Context
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.repository.NewsRepository
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks Firestore for a newly-posted announcement and, if there is one we haven't notified about
 * yet, posts a [NewsNotifier] notification (with the special announcement sound, in the user's
 * language). Runs on existing wake-ups — each prayer alarm, the daily refresh worker, and app open —
 * so it works on the free Firebase plan without Cloud Messaging. (Latency is until the next wake-up;
 * instant push would require FCM + a Cloud Function on the paid Blaze plan.)
 */
@Singleton
class NewsNotificationChecker @Inject constructor(
    private val newsRepository: NewsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun checkAndNotify(context: Context) {
        if (!settingsRepository.getNewsNotificationsEnabled()) return

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

        // Prefer the persisted language tag (reliable on a cold background wake, where
        // AppCompatDelegate.getApplicationLocales() can read empty); fall back to the live value.
        val lang = settingsRepository.getLanguageTag() ?: LocaleController.current().tag
        NewsNotifier.post(context, newest, lang)
        settingsRepository.setLastNotifiedNewsMillis(watermark)
    }
}
