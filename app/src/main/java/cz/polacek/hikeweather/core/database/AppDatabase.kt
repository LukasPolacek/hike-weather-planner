package cz.polacek.hikeweather.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cz.polacek.hikeweather.feature.trips.data.Converters
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.trips.data.TripDao

/**
 * Lokalni SQLite databaze pres Room
 * pri zmene struktury tabulky se zvysi version a doplni migrace
 */
@Database(entities = [Trip::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "hike_weather.db").build()
    }
}
