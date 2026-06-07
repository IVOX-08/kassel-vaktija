package de.igbdsandzakkassel.vaktija.ui.dashboard

import de.igbdsandzakkassel.vaktija.data.model.Prayer
import java.time.LocalTime

/** One prayer row on the dashboard. [isHighlighted] marks the NEXT upcoming prayer. */
data class PrayerRowUi(
    val prayer: Prayer,
    val adhan: LocalTime,
    val iqamah: LocalTime?,
    val isHighlighted: Boolean,
)

/** Everything the dashboard renders, recomputed once per second by the ViewModel. */
data class DashboardUiState(
    val loading: Boolean = true,
    val clock: String = "",
    val gregorianDate: String = "",
    val hijriDate: String = "",
    val rows: List<PrayerRowUi> = emptyList(),
    val nextPrayer: Prayer? = null,
    val countdown: String = "",
    val jumua: LocalTime? = null,
    val isStale: Boolean = false,
)
