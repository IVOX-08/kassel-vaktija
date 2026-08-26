package de.igbdsandzakkassel.vaktija.data.model

/**
 * A record that an admin account was used on a community it does not administer.
 *
 * Deliberately holds ids rather than names: names are looked up against the live catalogue when
 * shown, so a community that renames itself does not leave old alerts naming something that no
 * longer exists.
 */
data class AdminAlert(
    val id: String,
    val ownCommunityId: String,
    val attemptedCommunityId: String,
    val createdAt: Long,
)
