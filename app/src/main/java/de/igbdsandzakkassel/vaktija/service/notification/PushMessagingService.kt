package de.igbdsandzakkassel.vaktija.service.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives instant announcement pushes via Firebase Cloud Messaging. When the app is in the
 * background the system tray shows the Cloud Function's notification automatically (it sets the news
 * channel id via the manifest meta-data); this handles the foreground case, reusing the announcements
 * channel + tone. Falls back to the data payload if no notification block is present.
 *
 * NOTE: pushes only arrive once the accompanying Cloud Function is deployed (requires the Firebase
 * Blaze plan). Until then this service is dormant and the existing poll-on-wake check still delivers
 * announcements (just not instantly).
 */
class PushMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"].orEmpty()
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        NewsNotifier.postRaw(this, title, body)
    }
}
