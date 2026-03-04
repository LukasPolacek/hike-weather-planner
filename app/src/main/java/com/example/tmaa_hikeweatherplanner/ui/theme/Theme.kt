package com.example.tmaa_hikeweatherplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
//barevny tema D.M.
private val DarkColorScheme = darkColorScheme(
    primary = LightSage,
    onPrimary = Black,
    primaryContainer = DarkForest, //karty v tmavym
    surfaceVariant = DarkForest,   //alt karty v tmavym
    background = DeepCharcoal,
    surface = DeepCharcoal,
    onBackground = White,
    onSurface = White
)
//barevny tema L.M.
private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = White,
    primaryContainer = SoftCream,  //karty ve svetlym
    surfaceVariant = SoftCream,    //alt karty ve svetlym
    background = White,
    surface = White,
    onBackground = Black,
    onSurface = Black
)

//funkce pro nastaveni tematu cele aplikace
@Composable
fun TMAAHikeWeatherPlannerTheme(
    //zjisteni jaky tema ma system
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    //vyber schématu podle nastaveni systemu
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}