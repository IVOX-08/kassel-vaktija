package de.igbdsandzakkassel.vaktija.data.community

import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.data.model.CommunityLocation
import de.igbdsandzakkassel.vaktija.data.model.CommunityStatus
import de.igbdsandzakkassel.vaktija.data.model.CommunitySelection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The catalogue of communities, read from Firestore so a newly-joined community appears without an
 * app update — the whole point, since communities pay to be listed and cannot wait on Play review.
 *
 * Locations are stored flat on the community document rather than as a sub-collection: a community
 * has a handful of towns at most, and keeping them in one document means one read (and one offline
 * cache entry) per community instead of one per town.
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val selectionRepository: CommunitySelectionRepository,
) {

    /** Only communities currently taking part — what the picker offers. */
    fun observeSelectable(): Flow<List<Community>> =
        observeAll().map { all -> all.filter { it.status.isListed } }

    /** Every community including switched-off ones; the selection has to resolve against these. */
    fun observeAll(): Flow<List<Community>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                val parsed = snapshot?.documents
                    ?.mapNotNull { it.toCommunity() }
                    ?.sortedBy { it.name.lowercase() }
                    .orEmpty()
                trySend(parsed.ifEmpty { CommunityCatalog.SEED })
            }
        awaitClose { registration.remove() }
    }

    /** The community + location this device follows, resolved against the catalogue. */
    fun observeSelection(): Flow<CommunitySelection?> =
        combine(observeAll(), selectionRepository.observe()) { all, ids ->
            // Fall back to the first entry if the stored id vanished (a community left the
            // programme) so the app never ends up pointing at nothing.
            val community = all.firstOrNull { it.id == ids.communityId } ?: all.firstOrNull()
            val location = community?.locations?.firstOrNull { it.id == ids.locationId }
                ?: community?.locations?.firstOrNull()
            if (community != null && location != null) CommunitySelection(community, location) else null
        }

    /** Just the location — what the times/alarm layer needs. */
    fun observeLocation(): Flow<CommunityLocation?> = observeSelection().map { it?.location }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCommunity(): Community? {
        val name = getString("name") ?: return null
        @Suppress("UNCHECKED_CAST")
        val rawLocations = get("locations") as? List<Map<String, Any?>> ?: emptyList()
        val locations = rawLocations.mapNotNull { raw ->
            val locId = raw["id"] as? String ?: return@mapNotNull null
            val slug = raw["vaktijaSlug"] as? String ?: return@mapNotNull null
            CommunityLocation(
                id = locId,
                name = raw["name"] as? String ?: locId,
                vaktijaSlug = slug,
                latitude = (raw["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (raw["longitude"] as? Number)?.toDouble() ?: 0.0,
                address = raw["address"] as? String,
            )
        }
        if (locations.isEmpty()) return null
        return Community(
            id = id,
            // Absent means active: a community document written by hand in the console must not
            // silently switch itself off. The older boolean is still honoured as "suspended" so a
            // document written against the first version of this format keeps its meaning.
            status = when {
                getString("status") != null -> CommunityStatus.from(getString("status"))
                getBoolean("active") == false -> CommunityStatus.SUSPENDED
                else -> CommunityStatus.ACTIVE
            },
            name = name,
            logoUrl = getString("logoUrl"),
            donationUrl = getString("donationUrl"),
            locations = locations,
        )
    }

    private companion object {
        const val COLLECTION = "communities"
    }
}
