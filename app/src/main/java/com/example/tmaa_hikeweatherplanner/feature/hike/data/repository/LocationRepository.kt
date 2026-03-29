package com.example.tmaa_hikeweatherplanner.feature.hike.data.repository

import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.LocationDao
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.SavedLocation
import kotlinx.coroutines.flow.Flow

/**
 * Repozitar pro data o vyletech
 * oddeluje zbytek aplikace od konkretnich dat v LocationDao
 * kdyztak by se pridalo ukladani na cloud server
 */
class LocationRepository(private val dao: LocationDao) {

    /**
     * ziska vsechny ulozene vylety z databaze
     * vraci flow ktery ceka na zmeny a automaticky vydava novy seznam pri zmene dat
     */
    fun getAllLocations(): Flow<List<SavedLocation>> {
        return dao.getAllLocations()
    }

    /**
     * vlozi novy vylet do databaze
     * suspend aby bezela asynchrone
     * vola se z  viewmodelscope a zajisti ze zapisovani na disk neblokuje vlakno s ui
     */
    suspend fun addLocation(location: SavedLocation) {
        dao.insert(location)
    }

    // smaze konkretni vylet z databaze
    suspend fun deleteLocation(location: SavedLocation) {
        dao.delete(location)
    }

    //smaze vsechny vylety z tabulky
    suspend fun clearAllLocations() {
        dao.deleteAll()
    }
}