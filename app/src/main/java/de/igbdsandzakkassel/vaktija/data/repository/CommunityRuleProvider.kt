package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import kotlinx.coroutines.flow.Flow

/**
 * Source of community-set rules (Fajr Iqamah, Jumua time, Iqamah offsets). Phase 4b backs this
 * with a Firestore document + Room cache; a default implementation is used until then.
 */
interface CommunityRuleProvider {
    fun observeRules(): Flow<CommunityRules>
}
