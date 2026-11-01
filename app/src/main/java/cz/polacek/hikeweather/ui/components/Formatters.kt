package cz.polacek.hikeweather.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Formatovani dat a casu pro zobrazeni v cestine */
object Formatters {

    private val czech = Locale.forLanguageTag("cs-CZ")
    private val dayFormat = DateTimeFormatter.ofPattern("EEEE d. M.", czech)
    private val shortDayFormat = DateTimeFormatter.ofPattern("EEE d. M.", czech)
    private val dateTimeFormat = DateTimeFormatter.ofPattern("d. M. yyyy H:mm", czech)
    private val timeFormat = DateTimeFormatter.ofPattern("H:mm", czech)

    /** napr. "sobota 14. 11." */
    fun day(date: LocalDate): String = dayFormat.format(date)

    /** napr. "so 14. 11." */
    fun shortDay(date: LocalDate): String = shortDayFormat.format(date)

    /** "Dnes", "Zítra", "Za 3 dny", "Za 12 dní", "Proběhl" */
    fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days < 0 -> "Proběhl"
            days == 0L -> "Dnes"
            days == 1L -> "Zítra"
            days in 2..4 -> "Za $days dny"
            else -> "Za $days dní"
        }
    }

    /** cesky tvar podle poctu: 1 vylet, 2-4 vylety, 0 a 5+ vyletu */
    fun plural(count: Int, one: String, few: String, many: String): String = when (count) {
        1 -> "$count $one"
        in 2..4 -> "$count $few"
        else -> "$count $many"
    }

    fun dateTime(epochMillis: Long): String =
        dateTimeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    /** cas z unix sekund (vychod/zapad slunce) */
    fun time(epochSeconds: Long): String =
        timeFormat.format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()))
}
