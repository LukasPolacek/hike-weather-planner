package cz.polacek.hikeweather.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Rozhrani pro OpenWeatherMap (bezplatny tarif)
 * aktualni pocasi a predpoved na 5 dni po 3 hodinach
 */
interface OpenWeatherApi {

    @GET("data/2.5/weather")
    suspend fun currentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "cz"
    ): CurrentWeatherDto

    @GET("data/2.5/forecast")
    suspend fun forecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "cz"
    ): ForecastDto

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
    }
}

// --- Datove modely odpovedi (gson je plni podle nazvu v JSONu) ---

data class CurrentWeatherDto(
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<ConditionDto>,
    @SerializedName("wind") val wind: WindDto?,
    @SerializedName("sys") val sys: SysDto?,
    @SerializedName("timezone") val timezoneOffsetSeconds: Int?,
    @SerializedName("dt") val timestamp: Long
)

data class ForecastDto(
    @SerializedName("list") val items: List<ForecastItemDto>,
    @SerializedName("city") val city: ForecastCityDto?
)

data class ForecastItemDto(
    // cas predpovedi v sekundach od 1970 (UTC)
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<ConditionDto>,
    @SerializedName("wind") val wind: WindDto?,
    // pravdepodobnost srazek 0-1
    @SerializedName("pop") val precipitationProbability: Double?
)

data class ForecastCityDto(
    @SerializedName("timezone") val timezoneOffsetSeconds: Int?
)

data class MainDto(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double?,
    @SerializedName("temp_min") val tempMin: Double?,
    @SerializedName("temp_max") val tempMax: Double?,
    @SerializedName("humidity") val humidity: Int?
)

data class ConditionDto(
    // kod stavu pocasi (2xx bourka, 3xx mrholeni, 5xx dest, 6xx snih, 7xx mlha, 800 jasno, 80x oblacno)
    @SerializedName("id") val id: Int,
    @SerializedName("description") val description: String
)

data class WindDto(
    @SerializedName("speed") val speed: Double?
)

data class SysDto(
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?
)
