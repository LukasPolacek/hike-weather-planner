package com.example.tmaa_hikeweatherplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// model pro uziv profil
data class UserProfile(
    val name: String,
    val birthDate: String
)

// Databazova tabulka
@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val city: String,
    val difficulty: String,
    val weather: String,
    val latitude: Double,
    val longitude: Double
)