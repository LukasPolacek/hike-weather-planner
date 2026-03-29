package com.example.tmaa_hikeweatherplanner.feature.hike.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * rozhrani data accres objekt pro komunikaci s room databazi
 * definice sql dotazu a metody pro praci s tabulkou ulozenych vyletu
 * room si pri kompilaci sama vygeneruje kod ktery metody vykona
 */
@Dao
interface LocationDao {

    // creni pomoci flow pro asynchroni a okamzity prekresleni ui
    // vraci vsechny vylety podle id sestupne od nejnovejsiho
    // suspend netreba flow bezi na pzoadi
    @Query("SELECT * FROM saved_locations ORDER BY id DESC")
    fun getAllLocations(): Flow<List<SavedLocation>>

    // zapis do databaze
    // suspend aby bezelo asynchrone a neblokovalo ui
    // vraci long id noveho zaznamu
    @Insert
    suspend fun insert(location: SavedLocation): Long

    // smazani konkretniho vyletu, zase suspend aby bezelo napozadi
    @Delete
    suspend fun delete(location: SavedLocation)

    // vymazani vsech zaznamu v tabulce saved_location pres SQL dotaz
    @Query("DELETE FROM saved_locations")
    suspend fun deleteAll()
}