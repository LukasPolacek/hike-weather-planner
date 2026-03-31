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
import com.example.tmaa_hikeweatherplanner.feature.hike.ui.MainApp
import com.example.tmaa_hikeweatherplanner.feature.setup.ui.InitialS
import com.example.tmaa_hikeweatherplanner.ui.theme.TMAAHikeWeatherPlannerTheme

/**
 * Hlavni aktivita app
 * Nacita uzivatelsky profil ze sharedpreferences
 * rozhoduje jestli ukaze nastavovaci obrazovku nebo spusti dashboard
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Zobrazeni pres celou obrazovku
        enableEdgeToEdge()

        // Ziskani instance sharedpreferences pro cteni a zapis dat
        // mode private aby data byla pouze v teto app
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Nacteni jmena a datumu narozeni, pokud neprojde vrati null
        val savedName = sharedPref.getString("userName", null)
        val savedDate = sharedPref.getString("userDate", null)

        // UI v jetpack compose
        setContent {
            TMAAHikeWeatherPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Promenne pro compose, pokud obe hodnoty ma preskoci uvodni nastaveni
                    var isSetupComplete by remember { mutableStateOf(savedName != null && savedDate != null) }
                    var userName by remember { mutableStateOf(savedName ?: "") }
                    var userDate by remember { mutableStateOf(savedDate ?: "") }

                    if (!isSetupComplete) {
                        // zobrazeni obrazovky s nastavenim profilu
                        InitialS(
                            // prijjima cely objekt userprofile z InitialS
                            onSetupComplete = { profile ->
                                // Editor pro zapis a vlozeni dat pres putstring a zapis pres apply na pozadi
                                with(sharedPref.edit()) {
                                    // Vytahnuti dat z objektu
                                    putString("userName", profile.name)
                                    putString("userDate", profile.birthDate)
                                    apply()
                                }

                                // aktualizace stavu ui po uspesnym ulozeni
                                userName = profile.name
                                userDate = profile.birthDate
                                isSetupComplete = true
                            }
                        )
                    } else {
                        // Zobrazeni hlavniho rozhrani, prehled, hledat, profil
                        MainApp(
                            userName = userName,
                            userDate = userDate,
                            onReset = {
                                // vymazanani ulozenych dat pri resetu v profil obrazovce
                                with(sharedPref.edit()) {
                                    clear()
                                    apply()
                                }
                                // vraceni na setup obrazovku
                                isSetupComplete = false
                            }
                        )
                    }
                }
            }
        }
    }
}