package com.example.tmaa_hikeweatherplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedLocation::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "hike_database"
                ).build().also { instance = it }
            }
        }
    }
}