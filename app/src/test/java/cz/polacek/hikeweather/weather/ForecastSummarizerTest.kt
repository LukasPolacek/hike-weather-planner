package cz.polacek.hikeweather.weather

import cz.polacek.hikeweather.feature.weather.ForecastSlot
import cz.polacek.hikeweather.feature.weather.ForecastSummarizer
import cz.polacek.hikeweather.feature.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ForecastSummarizerTest {

    private val praha = 3600 // UTC+1 (zimni cas)

    private fun slot(day: Int, hour: Int, temp: Double, condition: WeatherCondition, wind: Double = 2.0, pop: Double = 0.0) =
        ForecastSlot(
            // mistni cas -> UTC sekundy
            timestamp = LocalDateTime.of(2026, 11, day, hour, 0).toEpochSecond(ZoneOffset.ofTotalSeconds(praha)),
            temperature = temp,
            condition = condition,
            description = condition.name.lowercase(),
            windSpeed = wind,
            precipitationProbability = pop
        )

    @Test
    fun `useky se rozdeli podle mistniho data a berou se jen hodiny pres den`() {
        val slots = listOf(
            slot(14, 2, -5.0, WeatherCondition.SNOW),        // noc - nepocita se
            slot(14, 11, 6.0, WeatherCondition.CLEAR),
            slot(14, 14, 9.0, WeatherCondition.RAIN, wind = 8.0, pop = 0.7),
            slot(14, 17, 4.0, WeatherCondition.CLOUDY),
            slot(15, 11, 3.0, WeatherCondition.CLOUDY)
        )

        val days = ForecastSummarizer.summarize(slots, praha)

        assertEquals(2, days.size)
        val first = days[0]
        assertEquals(LocalDate.of(2026, 11, 14), first.date)
        assertEquals(4.0, first.minTemperature, 0.01)
        assertEquals(9.0, first.maxTemperature, 0.01)
        // nejhorsi stav dne rozhoduje
        assertEquals(WeatherCondition.RAIN, first.condition)
        assertEquals(8.0, first.maxWindSpeed, 0.01)
        assertEquals(0.7, first.maxPrecipitationProbability, 0.01)
    }

    @Test
    fun `den jen s nocnimi useky pouzije vsechny useky`() {
        val days = ForecastSummarizer.summarize(listOf(slot(16, 2, 1.0, WeatherCondition.FOG)), praha)
        assertEquals(WeatherCondition.FOG, days.single().condition)
    }

    @Test
    fun `predpoved pro den mimo dosah vrati null`() {
        val days = ForecastSummarizer.summarize(listOf(slot(14, 12, 5.0, WeatherCondition.CLEAR)), praha)
        assertNull(ForecastSummarizer.forDate(days, LocalDate.of(2026, 11, 30)))
        assertEquals(5.0, ForecastSummarizer.forDate(days, LocalDate.of(2026, 11, 14))!!.maxTemperature, 0.01)
    }
}
