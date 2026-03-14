package com.example.tmaa_hikeweatherplanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
import com.example.tmaa_hikeweatherplanner.ui.screens.DashboardS
import com.example.tmaa_hikeweatherplanner.ui.screens.ProfileS
import com.example.tmaa_hikeweatherplanner.ui.screens.SearchS
import kotlinx.coroutines.launch

// Hlavni kontejner - spodni navigace,stav dat
@Composable
fun MainApp(userName: String, userDate: String, onReset: () -> Unit) {
    // Promenna indexu aktualni zalozky 0-Prehled 1-Hledat 2-Profil
    var selectedTab by remember { mutableIntStateOf(0) }

    // Snackbar dole na obrazovce
    val snackbarHostState = remember { SnackbarHostState() }

    // Spousteni uloh (Snackbar)
    val scope = rememberCoroutineScope()

    // Seznam ulozenych vyletu sdili se na vsechny obrazovky
    val savedLocations = remember { mutableStateListOf<SavedLocation>() }

    // Rozlozeni, prostor mezi listy, vyskakovaci zpravy
    Scaffold(
        // Napojeni stavu na kontejner ktry ty zpravy zobrazti
        snackbarHost = { SnackbarHost(snackbarHostState) },

        // Spodni lista na prapinani obrazovek
        bottomBar = {
            NavigationBar {
                // Ruzny zalozky navigace
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Přehled") },
                    label = { Text("Přehled") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Hledat") },
                    label = { Text("Hledat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") }
                )
            }
        }
    ) { paddingValues ->
        // Hlavni prostor pro obsah aplikace, systemove odsazeny od navigacni listy
        Box(modifier = Modifier.padding(paddingValues)) {

            // Prepinac obrazovek
            when (selectedTab) {
                // Dashboard
                0 -> DashboardS(
                    userName = userName,
                    recentLocations = savedLocations.take(2)
                )

                // Search
                1 -> SearchS(
                    recentLocations = savedLocations.take(2),
                    // Kdyz klikne na ulozeni vyletu
                    onLocationSaved = { city, diff, weather, lat, lon ->

                        // Kontrola duplicity podle gps souradnic
                        val isDuplicate = savedLocations.any { it.latitude == lat && it.longitude == lon }

                        if (isDuplicate) {
                            // Pokud jo vypise hlasku a neulzi
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Tato lokalita je již uložena.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            // Pokud ne ulozi na zacatek seznamu
                            savedLocations.add(0, SavedLocation(city, diff, weather, lat, lon))
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Výlet do $city byl uložen.",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                )

                // Profile
                2 -> ProfileS(
                    userName = userName,
                    userDate = userDate,
                    locations = savedLocations,
                    // Odstraneni jenoho vylezu ze seznamu
                    onDeleteLocation = { loc ->
                        savedLocations.remove(loc)
                        scope.launch { snackbarHostState.showSnackbar("Výlet ${loc.city} odstraněn.", duration = SnackbarDuration.Long) }
                    },
                    // Smazani vsech dat a navrat na nastavovaci obrazovku
                    onReset = {
                        savedLocations.clear()
                        scope.launch { snackbarHostState.showSnackbar("Všechna data smazána.", duration = SnackbarDuration.Long) }
                        onReset()
                    }
                )
            }
        }
    }
}