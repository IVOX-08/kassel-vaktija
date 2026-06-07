package de.igbdsandzakkassel.vaktija.core.device

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

/**
 * True when running on Android TV / Fire TV (leanback). The TV build routes to a TV-only
 * Dashboard with no bottom navigation and disables notifications/DND (see Phase 0 plan).
 */
fun Context.isTelevision(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
