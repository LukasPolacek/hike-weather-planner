package cz.polacek.hikeweather.feature.weather

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Prevod 3hodinove predpovedi na shrnuti po dnech
 * pro vylet se berou jen useky pres den (8-20 h mistniho casu)
 */
object ForecastSummarizer {

    private const val DAY_START_HOUR = 8
    private const val DAY_END_HOUR = 20

    fun summarize(slots: List<ForecastSlot>, timezoneOffsetSeconds: Int): List<DayForecast> {
        val offset = ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds)

        // rozdeleni useku podle mistniho data
        val byDate = slots.groupBy { slot ->
            Instant.ofEpochSecond(slot.timestamp).atOffset(offset).toLocalDate()
        }

        return byDate.map { (date, daySlots) ->
            // useky pres den, pokud zadne nejsou (napr. konec predpovedi), vezmou se vsechny
            val daytime = daySlots.filter { slot ->
                val hour = Instant.ofEpochSecond(slot.timestamp).atOffset(offset).hour
                hour in DAY_START_HOUR..DAY_END_HOUR
            }.ifEmpty { daySlots }

            // nejhorsi stav pocasi behem dne rozhoduje o popisu
            val worst = daytime.maxBy { it.condition.ordinal }

            DayForecast(
                date = date,
                minTemperature = daytime.minOf { it.temperature },
                maxTemperature = daytime.maxOf { it.temperature },
                condition = worst.condition,
                description = worst.description,
                maxWindSpeed = daytime.maxOf { it.windSpeed },
                maxPrecipitationProbability = daytime.maxOf { it.precipitationProbability }
            )
        }.sortedBy { it.date }
    }

    /** predpoved pro konkretni den, null pokud je mimo dosah predpovedi (vic nez 5 dni) */
    fun forDate(days: List<DayForecast>, date: LocalDate): DayForecast? =
        days.firstOrNull { it.date == date }
}
