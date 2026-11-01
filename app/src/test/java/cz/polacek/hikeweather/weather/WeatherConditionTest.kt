package cz.polacek.hikeweather.weather

import cz.polacek.hikeweather.feature.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun `kody OpenWeather se prevedou na stav pocasi`() {
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromCode(211))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCondition.fromCode(301))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromCode(502))
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromCode(601))
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromCode(741))
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromCode(800))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromCode(802))
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromCode(804))
    }
}
