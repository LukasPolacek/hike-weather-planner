package com.example.tmaa_hikeweatherplanner.data

import com.google.gson.annotations.SerializedName

// Darova trida pro odpoved z OpenWeather API
data class WeatherResponse(
    // Objekt s teplotou, tlakem
    @SerializedName("main") val main: Main,

    // Seznam s detaily o pocasi
    @SerializedName("weather") val weather: List<WeatherDescription>,

    // Nazev mesta z API
    @SerializedName("name") val cityName: String
)

// Main z json odpovedi
data class Main(
    // Aktualni teplota
    @SerializedName("temp") val temp: Double
)

// Jeden prvek z pole weather v json odpovedi
data class WeatherDescription(
    // Slovni popis pocasi (zatazeno,jasno..)
    @SerializedName("description") val description: String
)