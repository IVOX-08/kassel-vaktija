package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import kotlinx.coroutines.flow.Flow

/**
 * Source of community-set rules (Fajr Iqamah, Jumua time, Iqamah offsets). Backed by a Firestore
 * document so the admin can edit them and every device picks up the change (cached offline).
 */
interface CommunityRuleProvider {
    fun observeRules(): Flow<CommunityRules>

    /** Admin-only: persist new rules to the backend (no-op without write permission). */
    suspend fun saveRules(rules: CommunityRules)
}
