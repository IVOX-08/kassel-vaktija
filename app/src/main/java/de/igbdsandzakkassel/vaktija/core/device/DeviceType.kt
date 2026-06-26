package de.igbdsandzakkassel.vaktija.core.device

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * True when running on Android TV / Fire TV (leanback). The TV build routes to a TV-only
 * Dashboard with no bottom navigation and disables notifications/DND (see Phase 0 plan).
 */
fun Context.isTelevision(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * True on a TV (leanback) device, checked via the hardware feature rather than the UI mode. This is
 * reliable from ANY context (incl. the application context used by background services), where
 * [isTelevision]'s UiModeManager reading can be unreliable — so it's the one to gate background work
 * like alarm scheduling, which must never run on the passive TV display board.
 */
fun Context.isLeanbackTv(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        packageManager.hasSystemFeature("android.hardware.type.television")
