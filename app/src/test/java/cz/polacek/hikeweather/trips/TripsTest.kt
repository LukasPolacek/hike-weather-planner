package cz.polacek.hikeweather.trips

import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.trips.data.containsSamePlace
import cz.polacek.hikeweather.ui.components.Formatters
import cz.polacek.hikeweather.ui.dashboard.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripsTest {

    private val today = LocalDate.of(2026, 11, 10)

    private fun trip(name: String, date: LocalDate?, lat: Double = 49.59, lon: Double = 17.25, created: Long = 0) =
        Trip(name = name, detail = "", latitude = lat, longitude = lon, plannedDate = date, createdAt = created)

    @Test
    fun `stejne misto ve stejny den je duplicita`() {
        val existing = listOf(trip("Svatý Kopeček", today))
        assertTrue(existing.containsSamePlace(trip("Kopeček", today, lat = 49.5903, lon = 17.2502)))
        assertFalse(existing.containsSamePlace(trip("Kopeček", today.plusDays(1))))
        assertFalse(existing.containsSamePlace(trip("Jinde", today, lat = 49.70)))
    }

    @Test
    fun `vylety se rozdeli na nadchazejici, bez data a probehle`() {
        val sections = DashboardViewModel.splitTrips(
            listOf(
                trip("pozdeji", today.plusDays(5)),
                trip("bez data stary", null, created = 1),
                trip("dnes", today),
                trip("vcera", today.minusDays(1)),
                trip("bez data novy", null, created = 2)
            ),
            today
        )
        assertEquals(listOf("dnes", "pozdeji"), sections.upcoming.map { it.name })
        assertEquals(listOf("bez data novy", "bez data stary"), sections.undated.map { it.name })
        assertEquals(listOf("vcera"), sections.past.map { it.name })
    }

    @Test
    fun `relativni popis dne`() {
        assertEquals("Dnes", Formatters.relativeDay(today, today))
        assertEquals("Zítra", Formatters.relativeDay(today.plusDays(1), today))
        assertEquals("Za 3 dny", Formatters.relativeDay(today.plusDays(3), today))
        assertEquals("Za 7 dní", Formatters.relativeDay(today.plusDays(7), today))
        assertEquals("Proběhl", Formatters.relativeDay(today.minusDays(1), today))
    }

    @Test
    fun `cesky tvar podle poctu`() {
        assertEquals("1 výlet", Formatters.plural(1, "výlet", "výlety", "výletů"))
        assertEquals("3 výlety", Formatters.plural(3, "výlet", "výlety", "výletů"))
        assertEquals("0 výletů", Formatters.plural(0, "výlet", "výlety", "výletů"))
        assertEquals("12 výletů", Formatters.plural(12, "výlet", "výlety", "výletů"))
    }
}
