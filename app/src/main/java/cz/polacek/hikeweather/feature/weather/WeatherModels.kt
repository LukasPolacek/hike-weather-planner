package cz.polacek.hikeweather.feature.weather

import java.time.LocalDate

/**
 * Zjednoduseny stav pocasi podle kodu OpenWeather
 * poradi odpovida zavaznosti pro vylet (cim dal, tim horsi)
 */
enum class WeatherCondition {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDERSTORM;

    companion object {
        /** prevod kodu OpenWeather (https://openweathermap.org/weather-conditions) */
        fun fromCode(code: Int): WeatherCondition = when (code) {
            in 200..299 -> THUNDERSTORM
            in 300..399 -> DRIZZLE
            in 500..599 -> RAIN
            in 600..699 -> SNOW
            in 700..799 -> FOG
            800 -> CLEAR
            801, 802 -> PARTLY_CLOUDY
            else -> CLOUDY
        }
    }
}

/** Aktualni pocasi v miste */
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double?,
    val description: String,
    val condition: WeatherCondition,
    val windSpeed: Double?,
    val humidity: Int?,
    // vychod a zapad slunce v sekundach od 1970 (UTC)
    val sunrise: Long?,
    val sunset: Long?
) {
    /** kratky text pro seznamy a notifikace, napr. "8 °C, Oblačno" */
    val summary: String get() = "${temperature.toRoundedInt()} °C, ${description.capitalizeFirst()}"
}

/** Jeden 3hodinovy usek predpovedi */
data class ForecastSlot(
    val timestamp: Long,
    val temperature: Double,
    val condition: WeatherCondition,
    val description: String,
    val windSpeed: Double,
    val precipitationProbability: Double
)

/** Shrnuti predpovedi pro jeden den (pres den, kdy se chodi na vylet) */
data class DayForecast(
    val date: LocalDate,
    val minTemperature: Double,
    val maxTemperature: Double,
    // nejhorsi stav behem dne
    val condition: WeatherCondition,
    val description: String,
    val maxWindSpeed: Double,
    val maxPrecipitationProbability: Double
) {
    val summary: String
        get() = "${minTemperature.toRoundedInt()}–${maxTemperature.toRoundedInt()} °C, ${description.capitalizeFirst()}"
}

fun Double.toRoundedInt(): Int = Math.round(this).toInt()

fun String.capitalizeFirst(): String = replaceFirstChar { it.uppercase() }
