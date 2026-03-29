package com.example.tmaa_hikeweatherplanner

import android.app.Application
import com.example.tmaa_hikeweatherplanner.core.database.AppDatabase
import com.example.tmaa_hikeweatherplanner.feature.hike.data.repository.LocationRepository
/**
 *  Hlavni trida ktera se spousti jako prvni
 *  Udrzuje globalni stav app a inicializuje databazi, repozitar v jedne instanci po celej beh
 */
class MyApplication : Application() {
    // Inicializace databaze az je fakt potrebna
    val database by lazy { AppDatabase.getDatabase(this) }
    // Inicializace repozitare s pristupem databazovym operacim LocatioDao
    val locationRepository by lazy { LocationRepository(database.locationDao()) }
}