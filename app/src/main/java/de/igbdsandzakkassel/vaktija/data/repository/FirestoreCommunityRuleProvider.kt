package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community rules backed by the Firestore document `config/community`. Everyone reads it (Firestore
 * caches it offline); only the admin can write (enforced by the server security rule). Missing or
 * malformed fields fall back to [CommunityRules.DEFAULT].
 */
@Singleton
class FirestoreCommunityRuleProvider @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val settingsRepository: SettingsRepository,
) : CommunityRuleProvider {

    private val docRef get() = firestore.collection(COLLECTION).document(DOCUMENT)

    override fun observeRules(): Flow<CommunityRules> = callbackFlow {
        trySend(CommunityRules.DEFAULT)
        val registration = docRef.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.takeIf { it.exists() }?.toCommunityRules() ?: CommunityRules.DEFAULT)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveRules(rules: CommunityRules) {
        // Stamp the edit time so other devices can detect the change and notify their users.
        val now = System.currentTimeMillis()
        // Fire-and-forget: Firestore commits to the local cache immediately and syncs to the server
        // when online (awaiting the Task would block indefinitely while offline).
        docRef.set(rules.toFirestoreMap() + ("updatedAt" to now))
        // Advance OUR own watermark so the admin device doesn't notify itself about its own change.
        settingsRepository.setLastSeenConfigMillis(now)
    }

    override suspend fun getUpdatedAt(): Long? =
        runCatching { docRef.get().await().getLong("updatedAt") }.getOrNull()

    private companion object {
        const val COLLECTION = "config"
        const val DOCUMENT = "community"
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
        )
    }

    private fun CommunityRules.toFirestoreMap(): Map<String, Any> = mapOf(
        "fajrIqamah" to fajrIqamah.format(TIME),
        "jumua" to jumua.format(TIME),
        "dhuhrOffsetMin" to dhuhrOffsetMin,
        "asrOffsetMin" to asrOffsetMin,
        "maghribOffsetMin" to maghribOffsetMin,
        "ishaOffsetMin" to ishaOffsetMin,
    )
}
