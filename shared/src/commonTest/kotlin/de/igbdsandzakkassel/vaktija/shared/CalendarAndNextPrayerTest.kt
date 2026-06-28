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
