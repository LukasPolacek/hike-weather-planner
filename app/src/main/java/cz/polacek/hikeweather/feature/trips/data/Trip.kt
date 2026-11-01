package cz.polacek.hikeweather.feature.trips.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate
import java.util.UUID

/** Narocnost vyletu */
enum class Difficulty(val label: String) {
    EASY("Lehká"),
    MEDIUM("Střední"),
    HARD("Náročná")
}

/**
 * Planovany vylet ulozeny v lokalni databazi
 * id je UUID, aby slo vylet jednoznacne sparovat se zalohou v cloudu
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
    val difficulty: Difficulty = Difficulty.EASY,
    // den vyletu, null = bez data
    val plannedDate: LocalDate? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // posledni zname pocasi a vysledek hlidani predpovedi
    val weatherSummary: String? = null,
    val forecastMaxTemperature: Double? = null,
    val alertTitle: String? = null,
    val alertDetail: String? = null,
    val forecastCheckedAt: Long? = null
)

/** Prevod typu, ktere SQLite neumi primo ulozit */
class Converters {
    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun difficultyToString(difficulty: Difficulty): String = difficulty.name

    @TypeConverter
    fun stringToDifficulty(value: String): Difficulty =
        Difficulty.entries.firstOrNull { it.name == value } ?: Difficulty.EASY
}
