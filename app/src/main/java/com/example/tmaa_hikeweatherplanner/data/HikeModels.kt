package com.example.tmaa_hikeweatherplanner.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//model pro uziv profil
@Parcelize
data class UserProfile(
    val name: String,
    val birthDate: String
) : Parcelable

//model pro uloz lokalitu(a souradnice) a jeji pocasi
@Parcelize
data class SavedLocation(
    val city: String,
    val difficulty: String,
    val weather: String,
    val latitude: Double,
    val longitude: Double
) : Parcelable