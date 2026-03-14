package com.example.tmaa_hikeweatherplanner.network

import com.example.tmaa_hikeweatherplanner.data.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Rozhrani pro komunikaci se serverem pocasi pomoci Retrofit
interface WeatherApiService {

    // HTTP GET pozadavek na konkretni endpoint API
    @GET("data/2.5/weather")
    // Asynchroni suspend pro stazeni dat na pozadi
    suspend fun getCurrentWeather(
        // Zemepisna sirka
        @Query("lat") lat: Double,
        // Zemepisna  delka
        @Query("lon") lon: Double,
        // API klic pro overeni pristupu k OpenWeatherMap
        @Query("appid") apiKey: String,
        // Metricke jednotky
        @Query("units") units: String = "metric",
        // Nastaveni popisu v cestine
        @Query("lang") lang: String = "cz"
    ): WeatherResponse // Fromat odpovedi

    // Objekt pro vytvoreni instance sluzby
    companion object {
        // Hlavni adresa serveru
        private const val BASE_URL = "https://api.openweathermap.org/"

        // Funkce vrati pripraveneho Retrofit klienta
        fun create(): WeatherApiService {
            return Retrofit.Builder()
                // Nastaveni hlavni adresy
                .baseUrl(BASE_URL)
                // Pridani konvertoru JSON na Kotlin objekty
                .addConverterFactory(GsonConverterFactory.create())
                // Sestaveni Retrofitu
                .build()
                // Vytvoreni a vraceni konkretni implementace
                .create(WeatherApiService::class.java)
        }
    }
}