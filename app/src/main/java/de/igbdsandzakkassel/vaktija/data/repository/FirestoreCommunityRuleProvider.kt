package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community rules backed by `communities/{id}/config/rules`. Everyone reads them (Firestore caches
 * them offline); only that community's own admin can write (enforced by the server security rule).
 * Missing or malformed fields fall back to [CommunityRules.DEFAULT].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreCommunityRuleProvider @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val settingsRepository: SettingsRepository,
    private val communityRepository: CommunityRepository,
) : CommunityRuleProvider {

    /**
     * Iqamah and Jumu'ah live UNDER the community now, not in one global document.
     *
     * They were global while the app served one community. Left that way, all twenty would have
     * shared Kassel's 05:15 and 15:00 — every community showing another's congregation times, with
     * nothing on screen to reveal it.
     */
    private fun docRef(communityId: String) =
        firestore.collection(COMMUNITIES).document(communityId)
            .collection(CONFIG).document(RULES)

    /** The community being viewed; rules follow the selection like everything else. */
    private suspend fun currentDocRef() =
        communityRepository.observeSelection().first()?.community?.id?.let { docRef(it) }

    override fun observeRules(): Flow<CommunityRules> =
        communityRepository.observeSelection().flatMapLatest { selection ->
            val id = selection?.community?.id
            if (id == null) flowOf(CommunityRules.DEFAULT) else observeRulesOf(id)
        }

    private fun observeRulesOf(communityId: String): Flow<CommunityRules> = callbackFlow {
        trySend(CommunityRules.DEFAULT)
        val registration = docRef(communityId).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.takeIf { it.exists() }?.toCommunityRules() ?: CommunityRules.DEFAULT)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveRules(rules: CommunityRules) {
        // Stamp the edit time so other devices can detect the change and notify their users.
        val now = System.currentTimeMillis()
        // Fire-and-forget: Firestore commits to the local cache immediately and syncs to the server
        // when online (awaiting the Task would block indefinitely while offline).
        val ref = currentDocRef() ?: return
        ref.set(rules.toFirestoreMap() + ("updatedAt" to now))
        // Advance OUR own watermark so the admin device doesn't notify itself about its own change.
        settingsRepository.setLastSeenConfigMillis(now)
    }

    override suspend fun getUpdatedAt(): Long? =
        runCatching { currentDocRef()?.get()?.await()?.getLong("updatedAt") }.getOrNull()

    override suspend fun getRules(): CommunityRules {
        val ref = currentDocRef() ?: return CommunityRules.DEFAULT
        return runCatching {
            // Cache-first so a background reschedule doesn't block on the network; the snapshot
            // listener keeps the cache fresh while the app is used.
            val snapshot = runCatching { ref.get(Source.CACHE).await() }.getOrNull()?.takeIf { it.exists() }
                ?: ref.get().await()
            snapshot.takeIf { it.exists() }?.toCommunityRules()
        }.getOrNull() ?: CommunityRules.DEFAULT
    }

    private companion object {
        const val COMMUNITIES = "communities"
        const val CONFIG = "config"
        const val RULES = "rules"
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    private fun DocumentSnapshot.toCommunityRules(): CommunityRules {
        val d = CommunityRules.DEFAULT
        fun time(field: String, fallback: LocalTime): LocalTime =
            getString(field)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: fallback
        fun offset(field: String, fallback: Long): Long = getLong(field) ?: fallback
        return CommunityRules(
            fajrIqamah = time("fajrIqamah", d.fajrIqamah),
            jumua = time("jumua", d.jumua),
            dhuhrOffsetMin = offset("dhuhrOffsetMin", d.dhuhrOffsetMin),
            asrOffsetMin = offset("asrOffsetMin", d.asrOffsetMin),
            maghribOffsetMin = offset("maghribOffsetMin", d.maghribOffsetMin),
            ishaOffsetMin = offset("ishaOffsetMin", d.ishaOffsetMin),
            bajramDate = getString("bajramDate")
                ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
            bajramTime = getString("bajramTime")
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        )
    }

    private fun CommunityRules.toFirestoreMap(): Map<String, Any> = buildMap {
        put("fajrIqamah", fajrIqamah.format(TIME))
        put("jumua", jumua.format(TIME))
        put("dhuhrOffsetMin", dhuhrOffsetMin)
        put("asrOffsetMin", asrOffsetMin)
        put("maghribOffsetMin", maghribOffsetMin)
        put("ishaOffsetMin", ishaOffsetMin)
        // saveRules uses a full (non-merge) set(), so simply OMITTING the fields removes a cleared
        // Bajram announcement from the document — and thus from every device.
        bajramDate?.let { put("bajramDate", it.toString()) }
        bajramTime?.let { put("bajramTime", it.format(TIME)) }
    }
}
