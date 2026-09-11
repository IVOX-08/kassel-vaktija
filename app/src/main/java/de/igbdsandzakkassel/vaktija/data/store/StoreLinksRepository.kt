package de.igbdsandzakkassel.vaktija.data.store

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the two store listings live — the links behind the QR codes on the TV board.
 *
 * These come from Firestore rather than from the build for one reason: the board hangs on a wall in
 * a mosque. Nobody opens it again after it is mounted, and an Android TV does not reliably install
 * updates on its own. A link baked into the APK would mean that the day the iPhone app goes live,
 * every single community would have to fetch an app update before its board could show the code.
 *
 * With the link in the database, the iPhone code appears on every board within seconds of the head
 * admin filling the field in — no new build, no Play review, no waiting.
 *
 * Document `config/apps`:
 * ```
 * androidUrl : String   (optional; falls back to [PLAY_URL])
 * iosUrl     : String   (empty or missing = the App Store listing is not live yet)
 * ```
 * The published rules already cover this: `match /config/{docId}` is readable by anyone and
 * writable only by the head admin, so nothing has to be republished for it.
 */
@Singleton
class StoreLinksRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    fun observe(): Flow<StoreLinks> = callbackFlow {
        // Emit the fallback at once so the board never waits on the network to draw its Android
        // code — that link has been fixed since the first release and cannot change.
        trySend(StoreLinks(android = PLAY_URL, ios = APP_STORE_URL))
        val registration = firestore.collection(CONFIG).document(APPS)
            .addSnapshotListener { snapshot, _ ->
                // A document that does not exist yet still arrives here — Firestore reports the
                // absence, from its offline cache as well. Ignoring it matters: otherwise the very
                // first listener callback, before anyone has ever filled the document in, would
                // wipe the fallback above and the board would be left with no Android code either.
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                trySend(
                    StoreLinks(
                        android = snapshot.getString("androidUrl")?.takeIf { it.isNotBlank() }
                            ?: PLAY_URL,
                        ios = snapshot.getString("iosUrl")?.takeIf { it.isNotBlank() }?.trim()
                            ?: APP_STORE_URL,
                    ),
                )
            }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val CONFIG = "config"
        private const val APPS = "apps"

        /** The Play listing. Fixed by the application id, which can never change once published. */
        const val PLAY_URL =
            "https://play.google.com/store/apps/details?id=de.igbdsandzakkassel.vaktija"

        /**
         * The App Store listing. Built in since the iPhone app went live on 4 September 2026 —
         * the numeric id Apple assigns is permanent, so there is nothing left to fill in.
         *
         * The short form on purpose: without the country segment it opens in the viewer's own
         * store (there are Bosnian and Turkish Apple accounts in these communities), and it is
         * four QR modules smaller than the long one with the app name in it — which on a wall
         * board means bigger squares and an easier scan.
         *
         * Firestore may still override it: a board picks up `config/apps` within seconds, so if
         * Apple ever changes something, no TV has to wait for an app update. That safety net is
         * why this stayed a repository instead of becoming a constant in the TV screen.
         */
        const val APP_STORE_URL = "https://apps.apple.com/app/id6803973938"
    }
}

/**
 * @param android link to the Play listing — always set.
 * @param ios link to the App Store listing, or empty while it is not live. Empty means the board
 *   shows a "coming soon" placeholder instead of a code, because a QR that scans into a dead page
 *   is worse for the community than no QR at all.
 */
data class StoreLinks(
    val android: String,
    val ios: String,
) {
    val iosLive: Boolean get() = ios.isNotBlank()
}
