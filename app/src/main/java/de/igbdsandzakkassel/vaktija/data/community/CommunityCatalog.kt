package de.igbdsandzakkassel.vaktija.data.community

import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.data.model.CommunityLocation

/**
 * The list of communities the app can offer, as a last-resort fallback.
 *
 * The real catalogue lives in Firestore, deliberately: communities pay to be listed, so adding one
 * must never mean shipping an app update and waiting for Play review. This bundled copy only covers
 * the home community, so a first launch with no network still lands somewhere sensible instead of
 * on an empty picker.
 */
object CommunityCatalog {

    const val KASSEL_ID = "sandzak-kassel"

    val SEED: List<Community> get() = listOf(
        KASSEL,
        ROSENHEIM,
    )

    private val KASSEL = Community(
            id = KASSEL_ID,
            name = "IGBD-Gemeinde Sandžak-Kassel",
            donationUrl = "https://www.paypal.com/donate?business=ikzsandzakkassel@gmail.com",
            address = "Schwanenweg 13, 34123 Kassel",
            locations = listOf(
                CommunityLocation(
                    id = "kassel",
                    name = "Kassel",
                    vaktijaSlug = "kassel",
                    // The mosque itself, not the city centre: these coordinates drive the Qibla
                    // bearing and the computed month calendar.
                    latitude = 51.3093,
                    longitude = 9.5132,
                ),
                CommunityLocation(
                    id = "hann-muenden",
                    name = "Hann. Münden",
                    vaktijaSlug = "hann-munden",
                    latitude = 51.4194,
                    longitude = 9.6524,
                ),
                CommunityLocation(
                    // vaktija.eu does not publish Korbach; Brilon is the nearest town it does
                    // cover, 19 km away — under a minute of difference at this latitude.
                    id = "korbach",
                    name = "Korbach",
                    vaktijaSlug = "brilon",
                    latitude = 51.2761,
                    longitude = 8.8735,
                ),
            ),
    )

    // A real second community, so the multi-community paths (picker with more than one entry,
    // a community with no donation link, times far enough away to be obviously different) can be
    // exercised before Firestore is populated. Public details from igbd-rosenheim.de.
    // TODO: drop from the seed once the Firestore catalogue is live — the seed is only meant to
    // keep a first launch with no network from showing an empty picker.
    private val ROSENHEIM = Community(
        id = "rosenheim",
        name = "IGBD-Gemeinde Rosenheim",
        address = "Burgfriedstraße 55, 83024 Rosenheim",
        locations = listOf(
            CommunityLocation(
                id = "rosenheim",
                name = "Rosenheim",
                vaktijaSlug = "rosenheim",
                latitude = 47.8664,
                longitude = 12.1131,
            ),
        ),
    )

    /** The location a pre-multi-community install is silently migrated onto. */
    val DEFAULT_LOCATION_ID = SEED.first().locations.first().id
}
