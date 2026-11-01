package cz.polacek.hikeweather.feature.weather

import cz.polacek.hikeweather.core.network.OpenWeatherApi

/** Zdroj pocasi - rozhrani kvuli moznosti podstrcit v testech falesna data */
interface WeatherRepository {
    suspend fun current(lat: Double, lon: Double): Result<CurrentWeather>
    suspend fun dailyForecast(lat: Double, lon: Double): Result<List<DayForecast>>
}

/** Pocasi z OpenWeatherMap */
class OpenWeatherRepository(
    private val api: OpenWeatherApi,
    private val apiKey: String
) : WeatherRepository {

    override suspend fun current(lat: Double, lon: Double): Result<CurrentWeather> = runCatching {
        val dto = api.currentWeather(lat, lon, apiKey)
        val condition = dto.weather.firstOrNull()
        CurrentWeather(
            temperature = dto.main.temp,
            feelsLike = dto.main.feelsLike,
            description = condition?.description.orEmpty(),
            condition = WeatherCondition.fromCode(condition?.id ?: 803),
            windSpeed = dto.wind?.speed,
            humidity = dto.main.humidity,
            sunrise = dto.sys?.sunrise,
            sunset = dto.sys?.sunset
        )
    }

    override suspend fun dailyForecast(lat: Double, lon: Double): Result<List<DayForecast>> = runCatching {
        val dto = api.forecast(lat, lon, apiKey)
        val slots = dto.items.map { item ->
            val condition = item.weather.firstOrNull()
            ForecastSlot(
                timestamp = item.timestamp,
                temperature = item.main.temp,
                condition = WeatherCondition.fromCode(condition?.id ?: 803),
                description = condition?.description.orEmpty(),
                windSpeed = item.wind?.speed ?: 0.0,
                precipitationProbability = item.precipitationProbability ?: 0.0
            )
        }
        ForecastSummarizer.summarize(slots, dto.city?.timezoneOffsetSeconds ?: 0)
    }
}
