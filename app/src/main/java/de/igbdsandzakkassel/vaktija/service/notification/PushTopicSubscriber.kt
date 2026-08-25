package de.igbdsandzakkassel.vaktija.service.notification

import com.google.firebase.messaging.FirebaseMessaging
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this device subscribed to the push channels it should be hearing.
 *
 * Two channels: one per community, so an announcement from Berlin never buzzes a phone in Kassel,
 * and one federation-wide channel for the head admin's notices. The community one has to follow the
 * selection — a device that switched community and stayed subscribed to the old one would keep
 * receiving announcements from a community the user no longer follows, with no way to make it stop.
 */
@Singleton
class PushTopicSubscriber @Inject constructor(
    private val communityRepository: CommunityRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var current: String? = null

    fun start() {
        val messaging = FirebaseMessaging.getInstance()
        runCatching { messaging.subscribeToTopic(TOPIC_ALL) }
        scope.launch {
            communityRepository.observeSelection()
                .distinctUntilChanged { old, new -> old?.community?.id == new?.community?.id }
                .collect { selection ->
                    val topic = selection?.community?.id?.let { TOPIC_PREFIX + it.sanitised() }
                    if (topic == current) return@collect
                    current?.let { runCatching { messaging.unsubscribeFromTopic(it) } }
                    topic?.let { runCatching { messaging.subscribeToTopic(it) } }
                    current = topic
                }
        }
    }

    /** FCM topic names allow only [a-zA-Z0-9-_.~%]; community ids are slugs but may gain others. */
    private fun String.sanitised(): String = replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")

    private companion object {
        const val TOPIC_ALL = "all_communities"
        const val TOPIC_PREFIX = "community_"
    }
}
