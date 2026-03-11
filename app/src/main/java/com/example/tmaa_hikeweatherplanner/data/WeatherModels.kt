package com.example.tmaa_hikeweatherplanner.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("name") val cityName: String
)

data class Main(
    @SerializedName("temp") val temp: Double
)

data class WeatherDescription(
    @SerializedName("description") val description: String
)