package cz.polacek.hikeweather.feature.backup

import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.feature.trips.data.Trip
import java.time.LocalDate

/**
 * Prevod vyletu na dokument pro Firestore a zpet
 * zalohuji se jen udaje zadane uzivatelem, pocasi se po obnoveni nacte znovu
 */
object TripBackupMapper {

    fun toMap(trip: Trip): Map<String, Any?> = mapOf(
        "name" to trip.name,
        "detail" to trip.detail,
        "latitude" to trip.latitude,
        "longitude" to trip.longitude,
        "difficulty" to trip.difficulty.name,
        "plannedDate" to trip.plannedDate?.toEpochDay(),
        "note" to trip.note,
        "createdAt" to trip.createdAt
    )

    /** vrati null, pokud dokument nema povinne udaje */
    fun fromMap(id: String, data: Map<String, Any?>): Trip? {
        val name = data["name"] as? String ?: return null
        val latitude = (data["latitude"] as? Number)?.toDouble() ?: return null
        val longitude = (data["longitude"] as? Number)?.toDouble() ?: return null
        return Trip(
            id = id,
            name = name,
            detail = data["detail"] as? String ?: "",
            latitude = latitude,
            longitude = longitude,
            difficulty = Difficulty.entries.firstOrNull { it.name == data["difficulty"] } ?: Difficulty.EASY,
            plannedDate = (data["plannedDate"] as? Number)?.toLong()?.let { LocalDate.ofEpochDay(it) },
            note = data["note"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
