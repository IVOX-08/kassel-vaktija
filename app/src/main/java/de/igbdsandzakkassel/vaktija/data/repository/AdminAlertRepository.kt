package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.data.model.AdminAlert
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records when an admin's credentials are used on a community that is not theirs, and lets the head
 * admin read those records.
 *
 * The owner asked for this and the reason is sound: he hands out one login per community, and an
 * account turning up somewhere else is either a mistake worth clearing up or a credential that has
 * been passed around. Either way he wants to know rather than find out later.
 *
 * The alert is written by the device that made the attempt, because that is the only place that
 * knows both halves — which community the account belongs to, and which one was open at the time.
 * It is not a security control: an attacker could simply not write it. It is a record for an
 * honest mistake and a trail for a dishonest one.
 */
@Singleton
class AdminAlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    /** Fire-and-forget, like the other admin writes: never let logging block a sign-in. */
    fun recordWrongCommunityLogin(uid: String, ownCommunityId: String, attemptedCommunityId: String) {
        firestore.collection(COLLECTION).document().set(
            mapOf(
                "type" to TYPE_WRONG_COMMUNITY,
                "uid" to uid,
                "ownCommunityId" to ownCommunityId,
                "attemptedCommunityId" to attemptedCommunityId,
                "createdAt" to System.currentTimeMillis(),
            ),
        )
    }

    /** Newest first. Only the head admin can read these — enforced by the Firestore rule. */
    fun observeAlerts(): Flow<List<AdminAlert>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.mapNotNull { doc ->
                    AdminAlert(
                        id = doc.id,
                        ownCommunityId = doc.getString("ownCommunityId") ?: return@mapNotNull null,
                        attemptedCommunityId = doc.getString("attemptedCommunityId")
                            ?: return@mapNotNull null,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                    )
                }?.sortedByDescending { it.createdAt }.orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    fun dismiss(id: String) {
        firestore.collection(COLLECTION).document(id).delete()
    }

    private companion object {
        const val COLLECTION = "admin_alerts"
        const val TYPE_WRONG_COMMUNITY = "wrong_community_login"
    }
}
