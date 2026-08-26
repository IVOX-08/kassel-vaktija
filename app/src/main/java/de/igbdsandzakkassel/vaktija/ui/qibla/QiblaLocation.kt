package de.igbdsandzakkassel.vaktija.ui.qibla

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Last known device position, for a Qibla that is right when you are away from home.
 *
 * Deliberately COARSE location only. The bearing to the Kaaba changes by well under a hundredth of
 * a degree over a kilometre, so a city-level fix is every bit as good as a precise one — and coarse
 * permission is both easier to grant and far less to justify in a store listing.
 *
 * It reads the last known fix rather than requesting a live one: the compass is used standing
 * still, an old fix from the same town is exactly as accurate, and it costs no battery and no wait.
 */
object QiblaLocation {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Latitude/longitude, or null when there is no permission or no fix yet. */
    @SuppressLint("MissingPermission") // guarded by hasPermission
    fun lastKnown(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        // Newest fix across the providers that are actually enabled; network usually answers
        // instantly indoors, where GPS would not.
        val fix = runCatching {
            manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return null
        return fix.latitude to fix.longitude
    }
}
