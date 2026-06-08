package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.auth.FirebaseAuth
import de.igbdsandzakkassel.vaktija.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-Auth-backed admin gate. The admin is the single account whose UID matches
 * [BuildConfig.ADMIN_UID]; only that account may edit community rules/news (also enforced by the
 * Firestore security rule on the server).
 */
@Singleton
class AdminController @Inject constructor(
    private val auth: FirebaseAuth,
) {
    /** Emits true while the signed-in user is the admin. */
    fun observeIsAdmin(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid == BuildConfig.ADMIN_UID)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Sign in; succeeds only if the account is the admin (otherwise signs out again). */
    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        if (auth.currentUser?.uid != BuildConfig.ADMIN_UID) {
            auth.signOut()
            error("Not the admin account")
        }
    }

    fun signOut() = auth.signOut()
}
