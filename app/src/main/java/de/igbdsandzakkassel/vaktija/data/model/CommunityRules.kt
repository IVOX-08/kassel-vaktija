package de.igbdsandzakkassel.vaktija.data.model

import java.time.LocalTime

/**
 * Community-set values that layer on top of the vaktija.eu times. In Phase 4b these come from
 * Firestore (admin-edited); for now a default instance is used.
 *
 * Confirmed Iqamah offsets: Dhuhr +10, Asr +10, Maghrib +5, Isha +0. Fajr Iqamah is a fixed
 * community time (not an offset). Jumua is a fixed community time.
 */
data class CommunityRules(
    val fajrIqamah: LocalTime,
    val jumua: LocalTime,
    val dhuhrOffsetMin: Long = 10,
    val asrOffsetMin: Long = 10,
    val maghribOffsetMin: Long = 5,
    val ishaOffsetMin: Long = 0,
) {
    /** Iqamah time for a prayer given its adhan time, or null if the prayer has no Iqamah. */
    fun iqamah(prayer: Prayer, adhan: LocalTime): LocalTime? = when (prayer) {
        Prayer.FAJR -> fajrIqamah
        Prayer.DHUHR -> adhan.plusMinutes(dhuhrOffsetMin)
        Prayer.ASR -> adhan.plusMinutes(asrOffsetMin)
        Prayer.MAGHRIB -> adhan.plusMinutes(maghribOffsetMin)
        Prayer.ISHA -> adhan.plusMinutes(ishaOffsetMin)
        Prayer.SUNRISE -> null
    }

    companion object {
        // TBD-community-rule: seed defaults until the admin sets real values in Firestore (Phase 4b).
        // Owner will provide final day-one values (Open Item #2).
        val DEFAULT = CommunityRules(
            fajrIqamah = LocalTime.of(4, 30),
            jumua = LocalTime.of(15, 0),
        )
    }
}
