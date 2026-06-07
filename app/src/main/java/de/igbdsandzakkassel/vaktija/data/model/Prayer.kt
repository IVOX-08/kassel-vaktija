package de.igbdsandzakkassel.vaktija.data.model

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.R

/**
 * The six daily time entries shown on the dashboard. SUNRISE is displayed but is not an
 * obligatory prayer (no Iqamah, never the "active" prayer).
 */
enum class Prayer(@param:StringRes val labelRes: Int, val hasIqamah: Boolean) {
    FAJR(R.string.prayer_fajr, true),
    SUNRISE(R.string.prayer_sunrise, false),
    DHUHR(R.string.prayer_dhuhr, true),
    ASR(R.string.prayer_asr, true),
    MAGHRIB(R.string.prayer_maghrib, true),
    ISHA(R.string.prayer_isha, true);

    companion object {
        /** The five obligatory prayers (used for active/next-prayer logic). */
        val OBLIGATORY = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}
