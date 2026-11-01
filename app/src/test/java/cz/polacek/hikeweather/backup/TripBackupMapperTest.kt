package cz.polacek.hikeweather.backup

import cz.polacek.hikeweather.feature.backup.TripBackupMapper
import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.feature.trips.data.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TripBackupMapperTest {

    @Test
    fun `vylet projde zalohou a obnovenim beze zmeny zadanych udaju`() {
        val trip = Trip(
            id = "abc",
            name = "Praděd",
            detail = "Vrchol · Jeseníky",
            latitude = 50.0832,
            longitude = 17.2309,
            difficulty = Difficulty.HARD,
            plannedDate = LocalDate.of(2026, 11, 21),
            note = "Sraz v 7:00 na Ovčárně",
            createdAt = 123L,
            weatherSummary = "neni v zaloze"
        )

        val restored = TripBackupMapper.fromMap("abc", TripBackupMapper.toMap(trip))

        // pocasi se nezalohuje, po obnoveni se nacte znovu
        assertEquals(trip.copy(weatherSummary = null), restored)
    }

    @Test
    fun `cisla z Firestore mohou prijit jako Long misto Double`() {
        val restored = TripBackupMapper.fromMap("x", mapOf("name" to "Bouzov", "latitude" to 49L, "longitude" to 16L))
        assertEquals(49.0, restored!!.latitude, 0.0)
        assertEquals(Difficulty.EASY, restored.difficulty)
        assertNull(restored.plannedDate)
    }

    @Test
    fun `dokument bez povinnych udaju se preskoci`() {
        assertNull(TripBackupMapper.fromMap("x", mapOf("name" to "Bez souradnic")))
    }
}
