package de.igbdsandzakkassel.vaktija.shared

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarAndNextPrayerTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val calc = PrayerTimesCalculator()

    @Test
    fun monthHasCorrectDayCount() {
        assertEquals(30, calc.month(2026, 4, berlin).size)   // April → 30
        assertEquals(28, calc.month(2026, 2, berlin).size)   // Feb 2026 → 28
        assertEquals(29, calc.month(2024, 2, berlin).size)   // Feb 2024 → 29 (leap year)
        assertEquals(31, calc.month(2026, 12, berlin).size)  // December → 31
    }

    @Test
    fun monthDaysAreCompleteAndOrdered() {
        val days = calc.month(2026, 4, berlin)
        assertEquals(1, days.first().date.dayOfMonth)
        assertEquals(30, days.last().date.dayOfMonth)
    }

    @Test
    fun nextPrayerReturnsValidInfo() {
        val n = nextPrayerNow()
        println("Next prayer now: ${n.name} at ${n.time} (in ${n.inMinutes} min)")
        assertTrue(n.name in listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"), "unexpected name: ${n.name}")
        assertTrue(n.inMinutes >= 0, "minutes should not be negative: ${n.inMinutes}")
        assertTrue(Regex("""\d{2}:\d{2}""").matches(n.time), "time not HH:MM: ${n.time}")
    }
}

/**
 * Die Koordinaten muessen wirklich durchschlagen.
 *
 * Vorher rechnete jede dieser Funktionen fuer Kassel, egal welche Gemeinde gewaehlt war. Der Test
 * vergleicht Hamburg mit Muenchen: gut fuenf Breitengrade Unterschied, im Sommer ueber eine halbe
 * Stunde beim Morgengebet. Waeren die Koordinaten wieder verdrahtet, kaeme zweimal dasselbe heraus.
 */
class CoordinatesReachTheCalculationTest {

    private val berlin = TimeZone.of("Europe/Berlin")
    private val hamburg = 53.5511 to 9.9937
    private val muenchen = 48.1351 to 11.5820

    @Test
    fun monthDiffersBetweenHamburgAndMunich() {
        val h = monthForDisplay(2026, 6, hamburg.first, hamburg.second)
        val m = monthForDisplay(2026, 6, muenchen.first, muenchen.second)
        assertEquals(h.size, m.size)
        assertTrue(h.first().fajr != m.first().fajr,
            "Fajr identisch (${h.first().fajr}) — die Koordinaten kommen nicht an")
    }

    @Test
    fun dashboardRowsDifferBetweenHamburgAndMunich() {
        val h = dashboardRowsForToday(hamburg.first, hamburg.second).first { it.name == "Fajr" }
        val m = dashboardRowsForToday(muenchen.first, muenchen.second).first { it.name == "Fajr" }
        assertTrue(h.adhan != m.adhan, "Fajr identisch (${h.adhan}) — die Koordinaten kommen nicht an")
    }

    @Test
    fun defaultStaysKassel() {
        val explicit = monthForDisplay(2026, 6,
            PrayerTimesCalculator.KASSEL_LAT, PrayerTimesCalculator.KASSEL_LNG)
        assertEquals(monthForDisplay(2026, 6).first().fajr, explicit.first().fajr)
    }
}
