package cz.polacek.hikeweather.weather

import cz.polacek.hikeweather.feature.weather.DayForecast
import cz.polacek.hikeweather.feature.weather.WeatherAlertType
import cz.polacek.hikeweather.feature.weather.WeatherAlerts
import cz.polacek.hikeweather.feature.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeatherAlertsTest {

    private fun day(
        condition: WeatherCondition = WeatherCondition.CLEAR,
        max: Double = 15.0,
        wind: Double = 3.0,
        pop: Double = 0.0
    ) = DayForecast(LocalDate.of(2026, 11, 14), max - 6, max, condition, "popis", wind, pop)

    @Test
    fun `pekne pocasi nema upozorneni`() {
        assertNull(WeatherAlerts.evaluate(day()))
    }

    @Test
    fun `bourka ma prednost pred vetrem a destem`() {
        val alert = WeatherAlerts.evaluate(day(WeatherCondition.THUNDERSTORM, wind = 15.0, pop = 0.9))
        assertEquals(WeatherAlertType.THUNDERSTORM, alert?.type)
    }

    @Test
    fun `dest jen pri dostatecne pravdepodobnosti`() {
        assertNull(WeatherAlerts.evaluate(day(WeatherCondition.RAIN, pop = 0.3)))
        assertEquals(WeatherAlertType.RAIN, WeatherAlerts.evaluate(day(WeatherCondition.RAIN, pop = 0.8))?.type)
    }

    @Test
    fun `silny vitr`() {
        assertEquals(WeatherAlertType.STRONG_WIND, WeatherAlerts.evaluate(day(wind = 13.0))?.type)
    }

    @Test
    fun `vyrazne ochlazeni proti predchozi kontrole`() {
        assertEquals(WeatherAlertType.TEMPERATURE_DROP, WeatherAlerts.evaluate(day(max = 8.0), previousMaxTemperature = 15.0)?.type)
        assertNull(WeatherAlerts.evaluate(day(max = 12.0), previousMaxTemperature = 15.0))
    }
}
