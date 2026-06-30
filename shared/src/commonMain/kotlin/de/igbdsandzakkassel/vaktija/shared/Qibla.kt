package de.igbdsandzakkassel.vaktija.shared

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Initial great-circle bearing from Kassel to the Kaaba in Mecca, in degrees (0 = North, 90 = East).
 * Pure math — shared by Android and iOS; the platform supplies the live compass heading on top.
 */
fun qiblaDegrees(): Double {
    val lat1 = KASSEL_LAT.rad(); val lon1 = KASSEL_LNG.rad()
    val lat2 = KAABA_LAT.rad(); val lon2 = KAABA_LNG.rad()
    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return (atan2(y, x) * 180.0 / PI + 360.0) % 360.0
}

private const val KASSEL_LAT = 51.3127
private const val KASSEL_LNG = 9.4797
private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262

private fun Double.rad(): Double = this * PI / 180.0
