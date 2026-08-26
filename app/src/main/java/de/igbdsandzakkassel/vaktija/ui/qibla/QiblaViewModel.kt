package de.igbdsandzakkassel.vaktija.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Supplies the Qibla bearing for the selected mosque.
 *
 * This used to be a compile-time constant, which was fine while the app served one town. Across
 * Germany the bearing spans about 12° (Aachen ≈ 125°, Görlitz ≈ 138°), so a fixed Kassel value
 * would put a community at either edge several degrees off — visible when a row lines up.
 */
@HiltViewModel
class QiblaViewModel @Inject constructor(
    communityRepository: CommunityRepository,
) : ViewModel() {

    /** Set when the reading is based on the device's own position rather than the mosque. */
    private val _deviceFix = MutableStateFlow<Pair<Double, Double>?>(null)

    /** True while the bearing comes from where the phone actually is. */
    val usingDeviceLocation: StateFlow<Boolean> = _deviceFix
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val bearing: StateFlow<Float> = combine(
        communityRepository.observeLocation(),
        _deviceFix,
    ) { mosque, fix ->
        when {
            // Where the phone is wins: someone travelling is the whole reason this exists. Within
            // Germany the difference is a degree or two, but Novi Pazar is 7 degrees off Kassel and
            // Istanbul is 21 — far enough to line a row up wrong.
            fix != null -> bearingToKaaba(fix.first, fix.second)
            mosque != null -> bearingToKaaba(mosque.latitude, mosque.longitude)
            else -> KASSEL_FALLBACK
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KASSEL_FALLBACK)

    /** Called once permission is in hand; a null fix simply leaves the mosque bearing in place. */
    fun useDeviceLocation(latitude: Double, longitude: Double) {
        _deviceFix.value = latitude to longitude
    }

    /** Back to the mosque's bearing. */
    fun useMosqueLocation() {
        _deviceFix.value = null
    }

    private companion object {
        const val KAABA_LAT = 21.4225
        const val KAABA_LNG = 39.8262

        /** Kassel's mosque — shown only in the moment before the catalogue resolves. */
        val KASSEL_FALLBACK = bearingToKaaba(51.3093, 9.5132)

        /** Initial great-circle bearing from a point to the Kaaba, in degrees clockwise from north. */
        fun bearingToKaaba(latitude: Double, longitude: Double): Float {
            val lat1 = Math.toRadians(latitude)
            val lng1 = Math.toRadians(longitude)
            val kaabaLat = Math.toRadians(KAABA_LAT)
            val dLng = Math.toRadians(KAABA_LNG) - lng1
            val y = sin(dLng) * cos(kaabaLat)
            val x = cos(lat1) * sin(kaabaLat) - sin(lat1) * cos(kaabaLat) * cos(dLng)
            return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
        }
    }
}
