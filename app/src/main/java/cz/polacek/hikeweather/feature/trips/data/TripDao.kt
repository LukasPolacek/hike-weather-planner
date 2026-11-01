package cz.polacek.hikeweather.feature.trips.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    // vylety s datem podle data, vylety bez data na konci od nejnovejsiho
    @Query("SELECT * FROM trips ORDER BY plannedDate IS NULL, plannedDate, createdAt DESC")
    fun observeAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: String): Flow<Trip?>

    @Query("SELECT * FROM trips")
    suspend fun getAll(): List<Trip>

    @Upsert
    suspend fun upsert(trip: Trip)

    @Upsert
    suspend fun upsertAll(trips: List<Trip>)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
