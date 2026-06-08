package de.igbdsandzakkassel.vaktija.data.model

/**
 * A single community announcement, posted by the admin and shown to every user in the News tab.
 * Backed by a Firestore document in the `news` collection (cached offline). [createdAt] is a
 * client-set epoch-millis timestamp so the list orders correctly even before a server round-trip.
 */
data class NewsItem(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
)
