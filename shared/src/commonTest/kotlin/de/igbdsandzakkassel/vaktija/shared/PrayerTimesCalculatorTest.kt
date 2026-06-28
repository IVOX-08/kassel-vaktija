package de.igbdsandzakkassel.vaktija.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertTrue

class PrayerTimesCalculatorTest {

    private val berlin = TimeZone.of("Europe/Berlin")

    // A well-behaved spring date (no high-latitude twilight edge cases) for a stable ordering check.
    private val date = LocalDate(2026, 4, 15)

    @Test
    fun computesSixOrderedPrayerTimesForKassel() {
        val times = PrayerTimesCalculator().compute(date, berlin)

        println(
            "Kassel prayer times on $date (platform: ${platformName()}):\n" +
                "  Fajr    ${times.fajr}\n" +
                "  Sunrise ${times.sunrise}\n" +
                "  Dhuhr   ${times.dhuhr}\n" +
                "  Asr     ${times.asr}\n" +
                "  Maghrib ${times.maghrib}\n" +
                "  Isha    ${times.isha}",
        )

        // The six daily moments must come in the correct chronological order.
        val ordered = listOf(
            times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha,
        )
        for (i in 1 until ordered.size) {
            assertTrue(
                ordered[i] > ordered[i - 1],
                "Prayer times out of order at index $i: $ordered",
            )
        }
    }

    @Test
    fun fajrIsBeforeSunrise() {
        val times = PrayerTimesCalculator().compute(date, berlin)
        assertTrue(times.fajr < times.sunrise, "Fajr ${times.fajr} should be before sunrise ${times.sunrise}")
    }
}
