package de.igbdsandzakkassel.vaktija.service.dnd

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puts the phone into Do Not Disturb around prayer time and restores it afterwards. Requires the
 * user to grant DND access (ACCESS_NOTIFICATION_POLICY); without it every call is a no-op.
 *
 * The phone's prior DND state is persisted before silencing so it can be restored even if the
 * process is killed between the start and end alarms.
 */
@Singleton
class DndController @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val notificationManager = context.getSystemService<NotificationManager>()!!

    fun hasAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    /** Remember the current DND state, then switch to priority-only (the standard "silent") mode. */
    suspend fun silence() {
        if (!hasAccess()) return
        settingsRepository.setSavedInterruptionFilter(notificationManager.currentInterruptionFilter)
        runCatching {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    /** Restore whatever DND state the phone had before [silence] (defaults to "off"). */
    suspend fun restore() {
        if (!hasAccess()) return
        val saved = settingsRepository.getSavedInterruptionFilter()
            ?: NotificationManager.INTERRUPTION_FILTER_ALL
        runCatching { notificationManager.setInterruptionFilter(saved) }
    }
}
