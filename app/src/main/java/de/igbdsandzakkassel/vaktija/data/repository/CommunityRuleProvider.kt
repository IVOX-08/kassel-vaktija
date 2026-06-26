package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import kotlinx.coroutines.flow.Flow

/**
 * Source of community-set rules (Fajr Iqamah, Jumua time, Iqamah offsets). Backed by a Firestore
 * document so the admin can edit them and every device picks up the change (cached offline).
 */
interface CommunityRuleProvider {
    fun observeRules(): Flow<CommunityRules>

    /**
     * One-shot read of the current rules (cache-first), falling back to [CommunityRules.DEFAULT].
     * Unlike [observeRules] — whose first emission is the DEFAULT seed — this returns the actual
     * configured values, so background work (e.g. scheduling the Friday Jumu'ah alarm) uses the real
     * jumua time, not the default.
     */
    suspend fun getRules(): CommunityRules

    /** Admin-only: persist new rules to the backend (no-op without write permission). */
    suspend fun saveRules(rules: CommunityRules)

    /**
     * Epoch-millis of the last admin edit to the rules (a `updatedAt` field written on save), or null
     * if absent/unreadable. Used by the background check to notify users when prayer times change.
     */
    suspend fun getUpdatedAt(): Long?
}
