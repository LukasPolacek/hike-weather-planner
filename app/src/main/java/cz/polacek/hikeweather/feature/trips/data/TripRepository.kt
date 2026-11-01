package cz.polacek.hikeweather.feature.trips.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

interface TripRepository {
    fun observeTrips(): Flow<List<Trip>>
    fun observeTrip(id: String): Flow<Trip?>
    suspend fun getTrips(): List<Trip>
    suspend fun save(trip: Trip)
    suspend fun saveAll(trips: List<Trip>)
    suspend fun delete(id: String)
    suspend fun deleteAll()
}

class RoomTripRepository(private val dao: TripDao) : TripRepository {
    override fun observeTrips() = dao.observeAll()
    override fun observeTrip(id: String) = dao.observeById(id)
    override suspend fun getTrips() = dao.getAll()
    override suspend fun save(trip: Trip) = dao.upsert(trip)
    override suspend fun saveAll(trips: List<Trip>) = dao.upsertAll(trips)
    override suspend fun delete(id: String) = dao.deleteById(id)
    override suspend fun deleteAll() = dao.deleteAll()
}

/**
 * Kontrola duplicity - stejne misto (do cca 100 m) na stejny den uz v seznamu je
 */
fun List<Trip>.containsSamePlace(trip: Trip): Boolean = any { existing ->
    existing.id != trip.id &&
        existing.plannedDate == trip.plannedDate &&
        abs(existing.latitude - trip.latitude) < 0.001 &&
        abs(existing.longitude - trip.longitude) < 0.001
}
