package com.example.tmaa_hikeweatherplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * Motivy cele aplikace
 * Pro svetly i tmavy rezim
 */

// Barvy pro tmavy rezim
private val DarkColorScheme = darkColorScheme(
    primary = LightSage,           // Svetly odstin pro hlavni prvky
    onPrimary = Black,             // Barva textu na hlavnich prvcich
    primaryContainer = DarkForest, // Pozadi karet vyletu
    surfaceVariant = DarkForest,   // Pozadi textovych poli
    background = DeepCharcoal,     // Celkove pozadi obrazovek
    surface = DeepCharcoal,        // Pozadi standardnich ploch
    onBackground = White,          // Barva textu na celkovem pozadi
    onSurface = White              // Barva textu na standardnich plochach
)

// Barvy pro svetly rezim
private val LightColorScheme = lightColorScheme(
    primary = SageGreen,           // Hlavni tmavsi zelena pro aktivni prvky
    onPrimary = White,             // Text na hlavnich prvcich
    primaryContainer = SoftCream,  // Kremove pozadi pro karty
    surfaceVariant = SoftCream,    // Kremove pozadi pro textova pole a dalsi plochy
    background = White,            // Bile pozadi obrazovek
    surface = White,               // Bile pozadi standardnich ploch
    onBackground = Black,          // Hlavni tmave texty
    onSurface = Black              // Tmave texty na plochach
)

/**
 * Composable pro aplikaci.
 * Nastaveni barevneho schema na zaklade nastaveni systemu
 */
@Composable
fun TMAAHikeWeatherPlannerTheme(
    // Detekce tmaveho rezimu
    darkTheme: Boolean = isSystemInDarkTheme(),
    // UI komponenty na ktere se to schema aplikuje
    content: @Composable () -> Unit
) {
    // Vyber schematu podle aktualniho stavu darkTheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Aplikovani schematu pres materialthemeu
    MaterialTheme(colorScheme = colorScheme, content = content)
}