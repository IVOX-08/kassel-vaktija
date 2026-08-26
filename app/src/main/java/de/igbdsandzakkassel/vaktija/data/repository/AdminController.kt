package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.BuildConfig
import de.igbdsandzakkassel.vaktija.data.community.CommunityCatalog
import de.igbdsandzakkassel.vaktija.data.model.AdminRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Who is signed in and what they may administer.
 *
 * The role is read from `admins/{uid}` in Firestore, not compiled into the app: communities join
 * and leave the programme, and their admins change with each committee, so granting access has to
 * be an edit in the console. The security rules read the same document, so the server enforces
 * exactly what the UI shows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AdminController @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {

    /** The signed-in account's role, re-read whenever the account or its admin document changes. */
    fun observeRole(): Flow<AdminRole> = observeUid().flatMapLatest { uid ->
        if (uid == null) flow { emit(AdminRole.None) } else observeRoleFor(uid)
    }

    private fun observeUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun observeRoleFor(uid: String): Flow<AdminRole> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(
                    AdminRole.from(
                        role = snapshot?.getString("role"),
                        communityId = snapshot?.getString("communityId"),
                    ).orDebugFallback(uid),
                )
            }
        awaitClose { registration.remove() }
    }

    /**
     * Debug builds only: stand in for a missing `admins` document so the admin screens can be
     * worked on before Firestore is populated.
     *
     * Which role it stands in as is chosen in Settings, because the two roles must be testable
     * SEPARATELY — the first version of this always returned Head, which silently made every
     * sign-in a head admin and made the community role impossible to try at all.
     *
     * Release builds get no shortcut: there an account has exactly the rights its document grants.
     */
    private fun AdminRole.orDebugFallback(uid: String): AdminRole {
        if (this != AdminRole.None || !BuildConfig.DEBUG || uid != BuildConfig.ADMIN_UID) return this
        return debugRole
    }

    /** The stand-in role used while Firestore has no admins collection. Debug builds only. */
    var debugRole: AdminRole = AdminRole.Community(CommunityCatalog.KASSEL_ID)
        set(value) {
            field = value
            // Nudge the listeners so the UI re-reads the role without a sign-out/in cycle.
            auth.currentUser?.let { auth.updateCurrentUser(it) }
        }

    /**
     * Signs in and reports the role. An account with no admin document is signed out again rather
     * than left dangling — it has no business holding a session in this app.
     */
    suspend fun signIn(email: String, password: String): Result<AdminRole> = runCatching {
        val uid = auth.signInWithEmailAndPassword(email.trim(), password).await()
            .user?.uid ?: error("Sign-in returned no account")
        val document = firestore.collection(COLLECTION).document(uid).get().await()
        val role = AdminRole.from(document.getString("role"), document.getString("communityId"))
            .orDebugFallback(uid)
        if (role == AdminRole.None) {
            auth.signOut()
            error("This account has no admin rights")
        }
        role
    }

    fun signOut() = auth.signOut()

    private companion object {
        const val COLLECTION = "admins"
    }
}
