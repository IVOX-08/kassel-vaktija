package de.igbdsandzakkassel.vaktija.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Makes sure this device always has a Firebase identity, signing in anonymously when nobody is
 * signed in.
 *
 * Reactions are why: a like has to belong to someone, or the same phone could tap the heart a
 * hundred times and there would be no way to take one back. An anonymous account gives each
 * install a stable id without asking anyone for an e-mail or a password — nothing changes for the
 * user, who never sees a login.
 *
 * It also re-signs-in after an admin signs out, so the app does not silently drop to having no
 * identity at all — which would leave the reaction buttons dead with no visible reason.
 */
@Singleton
class SessionManager @Inject constructor(
    private val auth: FirebaseAuth,
) {
    fun start() {
        ensureSignedIn()
        auth.addAuthStateListener { ensureSignedIn() }
    }

    private fun ensureSignedIn() {
        if (auth.currentUser != null) return
        auth.signInAnonymously().addOnFailureListener { e ->
            // Most likely cause: anonymous sign-in not enabled in the Firebase console. Everything
            // else keeps working; only reactions go quiet, so this is worth a log line rather than
            // a crash or a message the user cannot act on.
            Log.w(TAG, "Anonymous sign-in failed; reactions will be unavailable", e)
        }
    }

    private companion object {
        const val TAG = "SessionManager"
    }
}
