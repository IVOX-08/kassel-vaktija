package de.igbdsandzakkassel.vaktija.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrayerRowsTest {

    @Test
    fun returnsSixFormattedRows() {
        val rows = prayerRowsForToday()
        assertEquals(6, rows.size)
        assertEquals("Fajr", rows.first().name)
        assertEquals("Isha", rows.last().name)
        val hhmm = Regex("""\d{2}:\d{2}""")
        rows.forEach { assertTrue(hhmm.matches(it.time), "not HH:MM: ${it.name} -> ${it.time}") }
    }
}
