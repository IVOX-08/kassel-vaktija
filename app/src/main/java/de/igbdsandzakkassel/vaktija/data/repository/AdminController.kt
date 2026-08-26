package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.BuildConfig
import de.igbdsandzakkassel.vaktija.data.community.CommunityCatalog
import de.igbdsandzakkassel.vaktija.data.model.AdminRole
import kotlinx.coroutines.CancellationException
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
                    ),
                )
            }
        awaitClose { registration.remove() }
    }


    /**
     * Signs in and reports what happened.
     *
     * "Wrong community" is deliberately a distinct outcome from "wrong password": the account is
     * genuine, it simply administers somewhere else. Telling the person that plainly saves a round
     * of confused password resets — and it is also the moment worth reporting to the head admin,
     * since an admin's credentials being tried on another community is exactly what he asked to
     * hear about.
     */
    suspend fun signIn(
        email: String,
        password: String,
        viewingCommunityId: String?,
    ): SignInResult = try {
        val uid = auth.signInWithEmailAndPassword(email.trim(), password).await()
            .user?.uid ?: error("Sign-in returned no account")
        val document = firestore.collection(COLLECTION).document(uid).get().await()
        val role = AdminRole.from(document.getString("role"), document.getString("communityId"))
        when {
            role == AdminRole.None -> {
                auth.signOut()
                SignInResult.NoRights
            }
            role is AdminRole.Community && viewingCommunityId != null &&
                role.communityId != viewingCommunityId -> {
                // The session is KEPT: this account is a valid admin, just not here. Walking back
                // to their own community has to work without signing in again.
                SignInResult.WrongCommunity(role.communityId, viewingCommunityId)
            }
            else -> SignInResult.Success(role)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        SignInResult.Failed(e)
    }

    /** Outcome of an admin sign-in attempt. */
    sealed interface SignInResult {
        data class Success(val role: AdminRole) : SignInResult
        /** Valid admin account, but for a different community. Session is kept. */
        data class WrongCommunity(val ownCommunityId: String, val attemptedCommunityId: String) :
            SignInResult
        /** Authenticated, but the account has no admin document at all. Signed back out. */
        data object NoRights : SignInResult
        /** Wrong credentials, no network, etc. */
        data class Failed(val cause: Throwable) : SignInResult
    }

    fun signOut() = auth.signOut()

    /** The signed-in account, for attributing an alert to it. */
    fun currentUid(): String? = auth.currentUser?.uid

    private companion object {
        const val COLLECTION = "admins"
    }
}
