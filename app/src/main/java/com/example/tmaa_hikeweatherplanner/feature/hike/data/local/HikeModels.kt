package com.example.tmaa_hikeweatherplanner.feature.hike.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Soubor s deifnicemi datovych modelu pro aplikaci

// model pro uzivatelsky profil pro predavani dat mezi obrazovkama
data class UserProfile(
    val name: String,
    val birthDate: String
)

// datova tabulka pro historie vyletu
// entity aby room vedel ze je to tabulka v sqlite databazi
@Entity(tableName = "saved_locations")
data class SavedLocation(
    // unikatni klic autmaticky se inkrementujici
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // data ulozena ve sloupcich tabulky
    val city: String,
    val difficulty: String,
    val weather: String,
    val latitude: Double,
    val longitude: Double
)