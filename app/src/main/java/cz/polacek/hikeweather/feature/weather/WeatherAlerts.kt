package cz.polacek.hikeweather.feature.weather

/** Typ upozorneni na zhorseni pocasi, poradi = priorita (prvni je nejvaznejsi) */
enum class WeatherAlertType(val title: String) {
    THUNDERSTORM("Hrozí bouřky"),
    SNOW("Hrozí sněžení"),
    STRONG_WIND("Silný vítr"),
    RAIN("Pravděpodobně bude pršet"),
    TEMPERATURE_DROP("Výrazné ochlazení")
}

data class WeatherAlert(val type: WeatherAlertType, val detail: String)

/**
 * Vyhodnoceni, jestli je pocasi na den vyletu nebezpecne nebo se vyrazne zhorsilo
 * cista logika bez Androidu, aby sla otestovat unit testy
 */
object WeatherAlerts {

    const val RAIN_PROBABILITY_THRESHOLD = 0.6
    const val STRONG_WIND_MS = 12.0
    const val TEMPERATURE_DROP_C = 5.0

    /**
     * vrati nejvaznejsi upozorneni nebo null pokud je pocasi v poradku
     * @param previousMaxTemperature nejvyssi teplota z predchozi kontroly (pro zjisteni ochlazeni)
     */
    fun evaluate(forecast: DayForecast, previousMaxTemperature: Double? = null): WeatherAlert? {
        val alerts = buildList {
            if (forecast.condition == WeatherCondition.THUNDERSTORM) {
                add(WeatherAlert(WeatherAlertType.THUNDERSTORM, forecast.summary))
            }
            if (forecast.condition == WeatherCondition.SNOW) {
                add(WeatherAlert(WeatherAlertType.SNOW, forecast.summary))
            }
            if (forecast.maxWindSpeed >= STRONG_WIND_MS) {
                add(WeatherAlert(WeatherAlertType.STRONG_WIND, "Nárazy až ${forecast.maxWindSpeed.toRoundedInt()} m/s"))
            }
            val isWet = forecast.condition == WeatherCondition.RAIN || forecast.condition == WeatherCondition.DRIZZLE
            if (isWet && forecast.maxPrecipitationProbability >= RAIN_PROBABILITY_THRESHOLD) {
                val percent = Math.round(forecast.maxPrecipitationProbability * 100)
                add(WeatherAlert(WeatherAlertType.RAIN, "Pravděpodobnost srážek $percent %"))
            }
            if (previousMaxTemperature != null &&
                previousMaxTemperature - forecast.maxTemperature >= TEMPERATURE_DROP_C
            ) {
                add(
                    WeatherAlert(
                        WeatherAlertType.TEMPERATURE_DROP,
                        "Maximum ${previousMaxTemperature.toRoundedInt()} → ${forecast.maxTemperature.toRoundedInt()} °C"
                    )
                )
            }
        }
        return alerts.minByOrNull { it.type.ordinal }
    }
}
