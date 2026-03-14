package com.example.tmaa_hikeweatherplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.tmaa_hikeweatherplanner.ui.navigation.HikeNavigation
import com.example.tmaa_hikeweatherplanner.ui.theme.TMAAHikeWeatherPlannerTheme

// Hlavni aktivita slouzi jako vstupni bod pro UI
class MainActivity : ComponentActivity() {

    // Metoda volana systemem pri spusteni nebo preklopeni aplikace
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kresleni aplikace pres cely displej
        enableEdgeToEdge()

        // Obsah obrazovky pres Jetpack Compose
        setContent {
            // Aplikovani tema (barvy, pismo..)
            TMAAHikeWeatherPlannerTheme {

                // Zakladni plocha pres celou velikost displeje
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // Barva pozadi podle tema svetle/tmave
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Hlavni navigace resi prepinani obrazovek
                    HikeNavigation()
                }
            }
        }
    }
}