package com.example.tmaa_hikeweatherplanner.core.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Rozhrani pro komunikaci se serverem pocasi pomoci knih. Retrofit
 * Rika na jake url adresy se ma aplikace ptat a jake parametry ma posilat
 */
interface WeatherApiService {

    // HTTP GET pozadavek na konkretni endpoint API
    @GET("data/2.5/weather")
    // suspend funkce pro stazeni dat na pozadi aby neblokovala UI
    suspend fun getCurrentWeather(
        // zemepisna sirka z GPS
        @Query("lat") lat: Double,
        // zemepisna delka z GPS
        @Query("lon") lon: Double,
        // API klic pro pristup k OpenWeatherMap
        @Query("appid") apiKey: String,
        // Vynuceni stupni Celsia misto fahrenheita
        @Query("units") units: String = "metric",
        // pozadavek na vraceni textoveho popisu pocasi v cestine
        @Query("lang") lang: String = "cz"
    ): WeatherResponse // format odpovedi, json se automaticky prevadi na tenhle objekt

    // Factory pro vytvoreni instance sluzby kdekoliv v aplikaci
    companion object {
        // hlavni adresa serveru na kterou se pridavaji jednotlivy endpointy
        private const val BASE_URL = "https://api.openweathermap.org/"

        // funkce sestavi a vrati retrofit klienta k pouziti
        fun create(): WeatherApiService {
            return Retrofit.Builder()
                // nastaveni hlavni adresy serveru
                .baseUrl(BASE_URL)
                // pridani GSON konvertoru ktery automaticky prelozi stazeny JSON text na weatherresponse
                .addConverterFactory(GsonConverterFactory.create())
                // sestaveni celeho retrofit objektu
                .build()
                // vytvoreni a vraceni implementace rozhrani
                .create(WeatherApiService::class.java)
        }
    }
}