package com.example.tmaa_hikeweatherplanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.LocationDao
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.SavedLocation

/**
 * Trida pro vytvoreni a spravovani sqlite databaze pres room
 * spojuje datove modely entitty a pristupove metody dao
 */
// definice ake tabulky entitites databaze obsahuje a jakou ma verzi
// pri zmene struktury treba pridani sloupce se verze zvysi
@Database(entities = [SavedLocation::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    // metoda pres kterou room poskytuje pristup k sql dotazum
    abstract fun locationDao(): LocationDao

    // companion object  pro vytvoreni singletonu aby zajistila ze v aplikaci bude
    // za behu exstovat jedina instance databaze
    companion object {

        // Volatile aby zmeny prromenne byly viditelne pro vsechna vlakna
        // aby si ruzna vlakna nedrzela ruzne verze
        @Volatile private var instance: AppDatabase? = null

        // Funkce pro ziskani nebo vytvoreni instance databaze
        fun getDatabase(context: Context): AppDatabase {
            // pokud instance existuje rovnou vraci
            // pokud ne vstupuje do bloku synchonized
            return instance ?: synchronized(this) {
                // synchronized zajisti ze v jeden moment vstupuje jen jedno vlakno
                // ochrana proti tomu aby dve vlakna nezacala vytvaret databazi soucasne
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "hike_database" // nazev souboru databaze v telefonu
                ).build().also { instance = it } // vytvoreni databeze a ulozeni do promenne
            }
        }
    }
}