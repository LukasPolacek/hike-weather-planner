package com.example.tmaa_hikeweatherplanner.core.network

import com.google.gson.annotations.SerializedName

/**
 * Datove modely pro preklad json odpovedi z openweater api na kotlin objekt
 * knihovna gson automaticky naplni modely stazenymi daty
 */
data class WeatherResponse(
    // SerializedName aby gson vedel poj jakym klicem ma v jsonu hledat data

    // hlavni objekt se zakladnimi daty teplota, tlak...)
    @SerializedName("main") val main: Main,

    // seznam s informacemi o stavu pocasi
    @SerializedName("weather") val weather: List<WeatherDescription>,

    // nazev lokality ktere vratilo api podle zaslanych GPS souradnic
    @SerializedName("name") val cityName: String
)

// trida reprezentujici JSON objekt  uvnitr hlavni odpovedi
data class Main(
    // aktualni teplota ve stupnich celsia nastevneo v weatherapiservice
    @SerializedName("temp") val temp: Double
)

// trida reprezentujici jeden prvek z pole weather z JSON odpovedi.
data class WeatherDescription(
    // slovni popis pocasi v cestine nasteveno v weatherapiservice
    @SerializedName("description") val description: String
)