package de.igbdsandzakkassel.vaktija.service.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives instant pushes via Firebase Cloud Messaging:
 *  - a new announcement (the Cloud Function sends a notification block; the system tray shows it when
 *    backgrounded — this handles the foreground case), and
 *  - a prayer-time change (a `type=config` data message → we post a localized notification ourselves).
 *
 * NOTE: pushes only arrive once the accompanying Cloud Function is deployed (requires the Firebase
 * Blaze plan). Until then this service is dormant and the existing poll-on-wake check still delivers
 * both (just not instantly). Both paths reuse the same notification ids, so they never double-notify.
 */
@AndroidEntryPoint
class PushMessagingService : FirebaseMessagingService() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "config") {
            // Watermark-gate it like the poll does, so we don't double-notify (push + later poll) and
            // the admin who just made the change isn't notified about their own edit.
            runBlocking {
                val updatedAt = message.data["updatedAt"]?.toLongOrNull()
                val lastSeen = settingsRepository.getLastSeenConfigMillis()
                val alreadyHandled = updatedAt != null && lastSeen != null && updatedAt <= lastSeen
                if (!alreadyHandled) {
                    val lang = LocaleController.persistedTag(this@PushMessagingService)
                        ?: settingsRepository.getLanguageTag() ?: LocaleController.current().tag
                    NewsNotifier.postConfigUpdate(this@PushMessagingService, lang)
                    // Clamp to our own clock so a future-dated edit can't suppress later changes.
                    if (updatedAt != null) {
                        settingsRepository.setLastSeenConfigMillis(minOf(updatedAt, System.currentTimeMillis()))
                    }
                }
            }
            return
        }
        val title = message.notification?.title ?: message.data["title"].orEmpty()
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        NewsNotifier.postRaw(this, title, body)
    }
}
