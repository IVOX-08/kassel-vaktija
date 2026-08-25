package de.igbdsandzakkassel.vaktija.data.community

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.communityDataStore by preferencesDataStore(name = "community_selection")

/** Which community + location this device follows. */
data class SelectedIds(val communityId: String, val locationId: String)

/**
 * Stores the picked community and location.
 *
 * Everyone who already has the app installed chose Kassel implicitly by installing it, so an
 * install with nothing stored is migrated onto Kassel rather than being dropped into an empty
 * picker on update. Only genuinely new installs see the picker, which the onboarding decides via
 * [hasChosen].
 */
@Singleton
class CommunitySelectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.communityDataStore

    fun observe(): Flow<SelectedIds> = store.data.map { prefs ->
        SelectedIds(
            communityId = prefs[COMMUNITY_ID] ?: CommunityCatalog.KASSEL_ID,
            locationId = prefs[LOCATION_ID] ?: CommunityCatalog.DEFAULT_LOCATION_ID,
        )
    }

    /** False only on a fresh install — an updated install counts as having chosen Kassel. */
    suspend fun hasChosen(): Boolean = store.data.first()[COMMUNITY_ID] != null

    suspend fun select(communityId: String, locationId: String) {
        store.edit { prefs ->
            prefs[COMMUNITY_ID] = communityId
            prefs[LOCATION_ID] = locationId
        }
    }

    private companion object {
        val COMMUNITY_ID = stringPreferencesKey("community_id")
        val LOCATION_ID = stringPreferencesKey("location_id")
    }
}
