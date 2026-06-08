package de.igbdsandzakkassel.vaktija.service.dnd

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puts the phone into Do Not Disturb around prayer time and turns it off again afterwards. Requires
 * the user to grant DND access (ACCESS_NOTIFICATION_POLICY); without it every call is a no-op.
 *
 * [restore] deliberately always returns to "all allowed" (sound on) rather than replaying a saved
 * filter: `currentInterruptionFilter` can report INTERRUPTION_FILTER_UNKNOWN (0), which is NOT a
 * valid argument to setInterruptionFilter — restoring it would throw and leave the phone stuck in
 * DND indefinitely. Resuming normal sound after the window is also the expected behaviour.
 */
@Singleton
class DndController @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val notificationManager = context.getSystemService<NotificationManager>()!!

    fun hasAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    /** Switch to priority-only (the standard "silent"/DND mode). */
    fun silence() {
        if (!hasAccess()) return
        runCatching {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    /** Turn Do Not Disturb off again (sound back on). */
    fun restore() {
        if (!hasAccess()) return
        runCatching {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}
