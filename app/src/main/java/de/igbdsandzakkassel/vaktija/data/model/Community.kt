package de.igbdsandzakkassel.vaktija.data.model

/**
 * One IGBD member community — the unit that has an admin, a logo, a donation link and its own
 * announcements. A community may cover SEVERAL towns (Kassel administers Kassel, Hann. Münden and
 * Korbach), and one town may host SEVERAL communities (Berlin has two, run separately). Those two
 * facts are why community and location have to be modelled apart.
 */
data class Community(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val donationUrl: String? = null,
    val locations: List<CommunityLocation> = emptyList(),
    /**
     * Whether the community is currently taking part. Set false by the head admin when a community
     * leaves the programme: it drops out of the picker so nobody new joins it, and anyone already
     * following it is told, rather than being left wondering why announcements stopped.
     */
    val active: Boolean = true,
)

/**
 * A place people actually pray, and the unit prayer times belong to.
 *
 * [vaktijaSlug] is the town on vaktija.eu whose published times this location uses. It is usually
 * the location's own town, but not always: vaktija.eu covers ~1300 German towns and a few members
 * fall outside it (Korbach), so those point at the nearest covered town instead — 19 km away is
 * well under a minute of difference, and a real published time beats a locally computed guess.
 */
data class CommunityLocation(
    val id: String,
    val name: String,
    val vaktijaSlug: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)

/** What the user picked in the onboarding picker: a location, and the community that runs it. */
data class CommunitySelection(
    val community: Community,
    val location: CommunityLocation,
)
