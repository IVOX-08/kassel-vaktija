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

        /**
         * Everything a user can be reminded about. Sunrise is not a prayer and never gets an
         * Adhan — it is here because Fajr ENDS at sunrise, and many members pray it 10-30 minutes
         * beforehand on their way to work. A reminder before sunrise is a reminder that Fajr is
         * about to run out.
         */
        // entries, not OBLIGATORY + SUNRISE: that appended sunrise after Isha, while it belongs
        // between Fajr and Dhuhr — which is where the enum already has it, and where anyone reading
        // the settings list expects to find it.
        val NOTIFIABLE = entries.toList()

        /**
         * Whether the phone should go quiet around this time for the congregation.
         *
         * Every entry sounds the Adhan, sunrise included. Sunrise still does NOT silence the
         * phone: there is no gathering to be quiet for, and switching someone's ringer off at
         * sunrise would be a surprise nobody asked for.
         */
        val Prayer.silencesForCongregation: Boolean get() = this != SUNRISE
    }
}
