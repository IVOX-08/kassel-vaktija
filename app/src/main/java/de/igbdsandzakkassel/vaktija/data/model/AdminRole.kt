package de.igbdsandzakkassel.vaktija.data.model

/**
 * What the signed-in account is allowed to do.
 *
 * The rights are deliberately NOT a ladder with the head admin on top. They are two different jobs:
 * a community runs its own prayer life, and the operator runs the programme. The head admin can
 * reach every community with an announcement and can suspend or remove one, but he cannot touch
 * anyone's Iqamah — those times are the community's own religious decision, and an outsider
 * changing them would be wrong even if it were convenient.
 *
 * Roles live in Firestore under `admins/{uid}` rather than in the app, so granting or revoking one
 * is an edit in the console, not a release. The security rules read the same document.
 */
sealed interface AdminRole {

    /** Signed out, or an account with no admin document. */
    data object None : AdminRole

    /** Runs exactly one community: its prayer times and its announcements. Nothing else. */
    data class Community(val communityId: String) : AdminRole

    /**
     * Runs the programme. Announces to every community, and decides who takes part — but does not
     * administer anyone's prayer times.
     */
    data object Head : AdminRole

    /** May edit this community's Iqamah and Jumu'ah. Only its own admin — never the head admin. */
    fun canEditTimes(communityId: String?): Boolean =
        this is Community && communityId != null && communityId == this.communityId

    /** May post and delete this community's announcements. Again only its own admin. */
    fun canPostNews(communityId: String?): Boolean = canEditTimes(communityId)

    /** May announce to every community at once. */
    val canBroadcast: Boolean get() = this == Head

    /** May add a community, suspend it for non-payment, or remove it. */
    val canManageCommunities: Boolean get() = this == Head

    /** Whether this account has any administrative rights at all — drives the Settings section. */
    val isAdmin: Boolean get() = this != None

    companion object {
        const val ROLE_HEAD = "head"
        const val ROLE_COMMUNITY = "community"

        /** Anything unrecognised means no access. */
        fun from(role: String?, communityId: String?): AdminRole = when {
            role.equals(ROLE_HEAD, ignoreCase = true) -> Head
            role.equals(ROLE_COMMUNITY, ignoreCase = true) && !communityId.isNullOrBlank() ->
                Community(communityId)
            else -> None
        }
    }
}
