package com.example.tmaa_hikeweatherplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    // Čtení pomocí Flow pro asynchronní překreslování UI
    @Query("SELECT * FROM saved_locations ORDER BY id DESC")
    fun getAllLocations(): Flow<List<SavedLocation>>

    // Zápis přes suspend funkci vracející Long
    @Insert
    suspend fun insert(location: SavedLocation): Long

    // Mazání přes suspend funkci
    @Delete
    suspend fun delete(location: SavedLocation)

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAll()
}