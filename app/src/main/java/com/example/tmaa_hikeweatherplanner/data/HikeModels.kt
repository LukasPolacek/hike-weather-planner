package com.example.tmaa_hikeweatherplanner.data
//Model pro profil uzivatele
data class UserProfile(
    val name: String,
    val birthDate: String
)
//Model pro ulozenou lokalitu a jeji pocasi
data class SavedLocation(
    val city: String,
    val difficulty: String,
    val weather: String
)