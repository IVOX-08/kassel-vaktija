package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Default community rules until Firebase is wired (Phase 4b).
 * TBD-decision: replace with a Firestore-backed implementation + Room cache.
 */
class StubCommunityRuleProvider @Inject constructor() : CommunityRuleProvider {
    override fun observeRules(): Flow<CommunityRules> = flowOf(CommunityRules.DEFAULT)
}
