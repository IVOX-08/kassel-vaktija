package de.igbdsandzakkassel.vaktija.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import de.igbdsandzakkassel.vaktija.data.model.Reaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Likes and dislikes on announcements — the community asks "tap the heart if you are coming".
 *
 * Two pieces of state, deliberately:
 *  - `reactions/{uid}` under the announcement records what THIS device chose, so a reaction can be
 *    taken back or changed, and so the button can show as already pressed.
 *  - `likeCount` / `dislikeCount` on the announcement itself, maintained with atomic increments, so
 *    the list can show totals without reading one document per reader.
 *
 * The counts can in principle drift from the individual records — an increment that lands while its
 * companion write fails leaves them out of step. That is accepted: these are a show of hands, not
 * an audit. Getting it exactly right would need a Cloud Function, which needs the paid plan.
 */
@Singleton
class NewsReactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val communityRepository: CommunityRepository,
) {

    /** What this device chose for [item], or null. */
    fun observeMyReaction(item: NewsItem): Flow<Reaction?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val ref = reactionsOf(item, item.communityId)?.document(uid)
        if (ref == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val registration = ref.addSnapshotListener { snapshot, _ ->
            trySend(Reaction.from(snapshot?.getString("value")))
        }
        awaitClose { registration.remove() }
    }

    /**
     * Applies [choice] for this device: sets it, swaps it, or — when it is what was already
     * chosen — takes it back. Counts move with it.
     */
    suspend fun react(item: NewsItem, choice: Reaction) {
        val uid = auth.currentUser?.uid ?: return
        val communityId = item.communityId
            ?: communityRepository.observeSelection().first()?.community?.id
        val reactions = reactionsOf(item, communityId) ?: return
        val parent = parentOf(item, communityId) ?: return
        val myRef = reactions.document(uid)

        val previous = Reaction.from(
            runCatching { myRef.get().await().getString("value") }.getOrNull(),
        )
        val deltas = mutableMapOf<String, Any>()
        fun bump(reaction: Reaction, by: Long) {
            val field = if (reaction == Reaction.LIKE) FIELD_LIKE else FIELD_DISLIKE
            deltas[field] = FieldValue.increment(by)
        }

        when (previous) {
            choice -> {
                // Same button again: take it back.
                myRef.delete()
                bump(choice, -1)
            }
            null -> {
                myRef.set(mapOf("value" to choice.name.lowercase()))
                bump(choice, 1)
            }
            else -> {
                myRef.set(mapOf("value" to choice.name.lowercase()))
                bump(previous, -1)
                bump(choice, 1)
            }
        }
        // Fire-and-forget like the rest of the writes here: Firestore applies it locally at once,
        // so the number moves under the reader's finger even with no connection.
        parent.update(deltas)
    }

    private fun parentOf(item: NewsItem, communityId: String?) = when {
        item.isBroadcast -> firestore.collection(BROADCASTS).document(item.id)
        communityId != null -> firestore.collection(COMMUNITIES).document(communityId)
            .collection(NEWS).document(item.id)
        else -> null
    }

    private fun reactionsOf(item: NewsItem, communityId: String?) =
        parentOf(item, communityId)?.collection(REACTIONS)

    private companion object {
        const val COMMUNITIES = "communities"
        const val NEWS = "news"
        const val BROADCASTS = "broadcasts"
        const val REACTIONS = "reactions"
        const val FIELD_LIKE = "likeCount"
        const val FIELD_DISLIKE = "dislikeCount"
    }
}
