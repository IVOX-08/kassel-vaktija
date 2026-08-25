package de.igbdsandzakkassel.vaktija.data.model

/**
 * What the signed-in account is allowed to do.
 *
 * Replaces the single hard-coded UID that worked while the app served one community. Roles live in
 * Firestore under `admins/{uid}` rather than in the app, so granting or revoking one is an edit in
 * the console — not a release. The same document is what the security rules read, so the server and
 * the UI can never disagree about who may write what.
 */
sealed interface AdminRole {

    /** Signed out, or an account with no admin document. */
    data object None : AdminRole

    /** Administers exactly one community: its announcements, its Iqamah times, nothing else. */
    data class Community(val communityId: String) : AdminRole

    /**
     * The programme operator. Sees every community's announcements, can broadcast to all users, and
     * can administer any community — needed because someone has to be able to step in when a
     * community's own admin is unreachable.
     */
    data object Head : AdminRole

    /** True when this role may administer [communityId]. */
    fun canAdminister(communityId: String?): Boolean = when (this) {
        None -> false
        Head -> true
        is Community -> communityId != null && communityId == this.communityId
    }

    val isAdmin: Boolean get() = this != None

    companion object {
        const val ROLE_HEAD = "head"
        const val ROLE_COMMUNITY = "community"

        /** Builds a role from an `admins/{uid}` document. Anything unrecognised means no access. */
        fun from(role: String?, communityId: String?): AdminRole = when {
            role.equals(ROLE_HEAD, ignoreCase = true) -> Head
            role.equals(ROLE_COMMUNITY, ignoreCase = true) && !communityId.isNullOrBlank() ->
                Community(communityId)
            else -> None
        }
    }
}
