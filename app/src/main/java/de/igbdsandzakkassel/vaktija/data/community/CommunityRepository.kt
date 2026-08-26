package de.igbdsandzakkassel.vaktija.data.community

import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.BuildConfig
import kotlinx.coroutines.tasks.await
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

    /**
     * Set a community's participation status. Head admin only — the Firestore rule enforces that;
     * this just sends the write.
     *
     * Fire-and-forget, like the other admin writes in this app: Firestore commits to the local
     * cache at once and syncs when there is a connection, whereas awaiting the task would hang
     * whenever the phone is offline.
     */
    fun setStatus(communityId: String, status: CommunityStatus) {
        firestore.collection(COLLECTION).document(communityId)
            .update("status", status.name.lowercase())
    }

    /**
     * One-off: writes the bundled catalogue into Firestore.
     *
     * Twenty communities, each with a nested list of towns, is an hour of error-prone typing in the
     * console — and the data already exists in the app. `set` with merge so a document that has
     * since gained a logo, a donation link or a status is not flattened back to the seed.
     *
     * Debug builds only. In a release this would let a mishap overwrite the live catalogue with
     * whatever happened to be compiled in.
     */
    suspend fun importSeed(): Int {
        if (!BuildConfig.DEBUG) return 0
        var written = 0
        CommunityCatalog.SEED.forEach { community ->
            val data = mutableMapOf<String, Any>(
                "name" to community.name,
                "locations" to community.locations.map { location ->
                    mapOf(
                        "id" to location.id,
                        "name" to location.name,
                        "vaktijaSlug" to location.vaktijaSlug,
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                    )
                },
            )
            community.address?.let { data["address"] = it }
            community.email?.let { data["email"] = it }
            community.donationUrl?.let { data["donationUrl"] = it }
            community.logoUrl?.let { data["logoUrl"] = it }
            community.imamName?.let { data["imamName"] = it }
            community.imamPhone?.let { data["imamPhone"] = it }
            runCatching {
                firestore.collection(COLLECTION).document(community.id)
                    .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                written++
            }
        }
        return written
    }

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
            address = getString("address"),
            email = getString("email"),
            imamName = getString("imamName"),
            imamPhone = getString("imamPhone"),
            locations = locations,
        )
    }

    private companion object {
        const val COLLECTION = "communities"
    }
}
