package de.igbdsandzakkassel.vaktija.ui.dashboard

import androidx.annotation.StringRes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import java.time.LocalTime

/**
 * One prayer row on the dashboard. [isHighlighted] marks the NEXT upcoming prayer. [labelRes] is
 * usually the prayer's own name, but on Fridays the Dhuhr row is shown as Jumu'ah.
 */
data class PrayerRowUi(
    val prayer: Prayer,
    @param:StringRes val labelRes: Int,
    val adhan: LocalTime,
    val iqamah: LocalTime?,
    val isHighlighted: Boolean,
    /** True right now during this prayer's Adhan→Iqamah window (congregation gathering) — the card
     *  glows green-white while it's active. False outside the window / when there's no Iqamah. */
    val inIqamahWindow: Boolean = false,
)

/** Everything the dashboard renders, recomputed once per second by the ViewModel. */
data class DashboardUiState(
    val loading: Boolean = true,
    val clock: String = "",
    val gregorianDate: String = "",
    val hijriDate: String = "",
    val rows: List<PrayerRowUi> = emptyList(),
    val nextPrayer: Prayer? = null,
    /** Display label of the prayer the countdown targets (Jumu'ah on the Friday Dhuhr slot). */
    @param:StringRes val nextPrayerLabelRes: Int? = null,
    val countdown: String = "",
    val jumua: LocalTime? = null,
    /** Admin-announced Eid (Bajram) prayer — shown as a gold banner while today or upcoming. */
    val bajram: Pair<java.time.LocalDate, LocalTime>? = null,
    val isStale: Boolean = false,
    /** Selected town — shown in the header, since one community can span several towns. */
    val locationName: String = "",
    /** Street address of the selected town; null when that town has none on file yet. */
    val locationAddress: String? = null,
    /** The selected community's donation link. */
    val donationUrl: String? = null,
)
