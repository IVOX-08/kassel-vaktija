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
    /**
     * The community's mosque. This sits on the COMMUNITY, not on each town: Kassel administers
     * three towns but has one mosque, so someone whose times are set to Korbach still needs to be
     * pointed at Schwanenweg 13. A town may override it if a community ever runs a second mosque.
     */
    val address: String? = null,
    /** Contact address of the community office, shown in "About". */
    val email: String? = null,
    /** The community's own telephone and website, as IGBD's register lists them. */
    val phone: String? = null,
    val website: String? = null,
    /**
     * Where the community posts. Shown under the announcements, because that is where someone who
     * just read one is most likely to want more. A community that has not sent its own carries the
     * federation's accounts.
     */
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val youtubeUrl: String? = null,
    /** The community's imam, and the number people may call. Both optional. */
    val imamName: String? = null,
    val imamPhone: String? = null,
    val locations: List<CommunityLocation> = emptyList(),
    val status: CommunityStatus = CommunityStatus.ACTIVE,
)

/**
 * How far a community's listing is switched off. Set by the head admin and reversible at any time —
 * it is one field on the community document, so restoring a community is the same click as pausing
 * one.
 *
 * Two levels exist because the two reasons are not the same. A community that simply cannot pay
 * this month should lose its presence, not its members' prayer times; a community that is actively
 * causing trouble should be gone from the app entirely.
 */
enum class CommunityStatus {
    /** Listed and fully working. */
    ACTIVE,

    /**
     * Not paying. Drops out of the picker, loses its logo, donation link, announcements and TV
     * board — but prayer times keep working. The people praying did not withhold the money, and
     * the times come from a public website anyway.
     */
    SUSPENDED,

    /**
     * Removed. The app shows a notice instead of the community's content and offers to pick another
     * one. For communities disrupting the project rather than merely owing for it.
     */
    BLOCKED,
    ;

    val isListed: Boolean get() = this == ACTIVE
    /** Whether the community's own branding, donations and announcements are shown. */
    val showsCommunityContent: Boolean get() = this == ACTIVE

    companion object {
        /** Absent/unknown means active, so a hand-written document never switches itself off. */
        fun from(raw: String?): CommunityStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

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
    /** Only set when this town has its OWN mosque; otherwise the community's address is used. */
    val address: String? = null,
    /** Contact address of the community office, shown in "About". */
    val email: String? = null,
    /** The community's own telephone and website, as IGBD's register lists them. */
    val phone: String? = null,
    val website: String? = null,
    /**
     * Where the community posts. Shown under the announcements, because that is where someone who
     * just read one is most likely to want more. A community that has not sent its own carries the
     * federation's accounts.
     */
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val youtubeUrl: String? = null,
    /** The community's imam, and the number people may call. Both optional. */
    val imamName: String? = null,
    val imamPhone: String? = null,
)

/** What the user picked in the onboarding picker: a location, and the community that runs it. */
data class CommunitySelection(
    val community: Community,
    val location: CommunityLocation,
)
