package com.example.tmaa_hikeweatherplanner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.tmaa_hikeweatherplanner.ui.MainApp
import com.example.tmaa_hikeweatherplanner.ui.screens.InitialS
import com.example.tmaa_hikeweatherplanner.ui.theme.TMAAHikeWeatherPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Nacteni SharedPreferences identifikovanych jmenem v privatnim modu
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Nacteni dat ze SharedPreferences. Kdyz klic neni nalezen, vrati se null
        val savedName = sharedPref.getString("userName", null)
        val savedDate = sharedPref.getString("userDate", null)

        setContent {
            TMAAHikeWeatherPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pokud jsou obe hodnoty v poradku nacteny, preskakujeme uvodni nastaveni
                    var isSetupComplete by remember { mutableStateOf(savedName != null && savedDate != null) }
                    var userName by remember { mutableStateOf(savedName ?: "") }
                    var userDate by remember { mutableStateOf(savedDate ?: "") }

                    if (!isSetupComplete) {
                        InitialS(
                            onSetupComplete = { name, date ->
                                // Pro zapis vytvorime Editor a vlozime data pomoci putString
                                with(sharedPref.edit()) {
                                    putString("userName", name)
                                    putString("userDate", date)
                                    apply()
                                }

                                userName = name
                                userDate = date
                                isSetupComplete = true
                            }
                        )
                    } else {
                        MainApp(
                            userName = userName,
                            userDate = userDate,
                            onReset = {
                                // Vymazani vsech ulozenych preferenci pri resetu aplikace
                                with(sharedPref.edit()) {
                                    clear()
                                    apply()
                                }
                                isSetupComplete = false
                            }
                        )
                    }
                }
            }
        }
    }
}