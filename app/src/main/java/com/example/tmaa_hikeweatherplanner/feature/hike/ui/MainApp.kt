package com.example.tmaa_hikeweatherplanner.feature.hike.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * Hlavni kontejner po beh aplikace po zalozeni profilu
 * Stare se o navigacni listu a prepinani mezi obrazovkami prehled, hledat, profil
 * Propojuje datovou-viewmodel s jednotlivymi obrazovkama
 */
@Composable
fun MainApp(
    userName: String,
    userDate: String,
    onReset: () -> Unit,
    // viewmodel samo ziska ui diky factory v myapplication, zachova i pri otoceni displeje
    viewModel: HikeViewModel = viewModel(factory = HikeViewModel.Factory)
) {
    // Stav ktera zalozka je vybrana 0 Prehled 1 Hledat 2 Profil)
    var selectedTab by remember { mutableIntStateOf(0) }

    // stavy pro zobrazeni vyskakovacich zprav dole na obrazovce
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // sbirani stavu z viewmodelu
    // collectasstate kvuli tomu aby se pri zmene seznamu vyletu prekreslilo ui
    val savedLocations by viewModel.savedLocations.collectAsState()

    // scaffolt jako zaklad material designu pro pridani spodni navigace a mista pro vyskakovaci zpravy
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Přehled") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("Hledat") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profil") })
            }
        }
    ) { paddingValues ->
        // box pro obaleni obsahuji a nastaveni vnitrnich okraju paddingValues ze scaffoldu
        // aby spodni lista neprekryvala obsah obrazovky
        Box(modifier = Modifier.padding(paddingValues)) {
            // podle vybrane zalozky vykresli prislusny obsahg
            when (selectedTab) {
                // Dashboard ukazuji se jen posledni dva pridane vylety
                0 -> DashboardS(userName = userName, recentLocations = savedLocations.take(2))

                // Hledani lokaci
                1 -> SearchS(
                    recentLocations = savedLocations.take(2),
                    onLocationSaved = { city, diff, weather, lat, lon ->
                        // Kontrola jestli uz to misto neni pridane podle gps souradnic
                        val isDuplicate = savedLocations.any { it.latitude == lat && it.longitude == lon }

                        if (isDuplicate) {
                            scope.launch { snackbarHostState.showSnackbar("Tato lokalita je již uložena.") }
                        } else {
                            // ui vrstva data preda viewmodelu
                            viewModel.saveLocation(city, diff, weather, lat, lon)
                            scope.launch { snackbarHostState.showSnackbar("Výlet uložen.") }
                        }
                    }
                )

                // Profilova stranka
                2 -> ProfileS(
                    userName = userName,
                    userDate = userDate,
                    locations = savedLocations,
                    onDeleteLocation = { loc ->
                        // mazani obsahu se preda viewmodelu
                        viewModel.deleteLocation(loc)
                        scope.launch { snackbarHostState.showSnackbar("Výlet odstraněn.") }
                    },
                    onReset = {
                        // smazani vsech vyleztu z databaze
                        viewModel.clearAllData()
                        // zavolani funkce z mainactivity na smazani profilu a navrat na nastavovaci obrazovku
                        onReset()
                    }
                )
            }
        }
    }
}