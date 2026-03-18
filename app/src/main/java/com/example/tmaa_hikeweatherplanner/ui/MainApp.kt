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
import androidx.compose.ui.platform.LocalContext
import com.example.tmaa_hikeweatherplanner.data.AppDatabase
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
import com.example.tmaa_hikeweatherplanner.ui.screens.DashboardS
import com.example.tmaa_hikeweatherplanner.ui.screens.ProfileS
import com.example.tmaa_hikeweatherplanner.ui.screens.SearchS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Hlavni kontejner aplikace starajici se o spodni navigaci a spolecny stav dat
@Composable
fun MainApp(userName: String, userDate: String, onReset: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Inicializace databaze a DAO nastroju
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.locationDao() }

    // Sber dat z databaze pres reaktivni Flow (automaticky se aktualizuje pri zmene)
    val savedLocations by dao.getAllLocations().collectAsState(initial = emptyList())

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
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
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardS(
                    userName = userName,
                    recentLocations = savedLocations.take(2)
                )

                1 -> SearchS(
                    recentLocations = savedLocations.take(2),
                    onLocationSaved = { city, diff, weather, lat, lon ->

                        val isDuplicate = savedLocations.any { it.latitude == lat && it.longitude == lon }

                        if (isDuplicate) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Tato lokalita je již uložena.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            // Zapis do databaze musi bezet na pozadi, aby se nezaseklo UI (Dispatchers.IO)
                            scope.launch(Dispatchers.IO) {
                                // Pouzivame pojmenovane argumenty (city = city...), aby se id nechalo vygenerovat samo
                                dao.insert(
                                    SavedLocation(
                                        city = city,
                                        difficulty = diff,
                                        weather = weather,
                                        latitude = lat,
                                        longitude = lon
                                    )
                                )
                                // Zobrazeni vyskakovaci zpravy musime vratit zpet na hlavni UI vlakno
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(
                                        message = "Výlet do $city byl uložen.",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        }
                    }
                )

                2 -> ProfileS(
                    userName = userName,
                    userDate = userDate,
                    locations = savedLocations,
                    onDeleteLocation = { loc ->
                        // Mazani jednoho vyletu z databaze na pozadi
                        scope.launch(Dispatchers.IO) {
                            dao.delete(loc)
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar("Výlet ${loc.city} odstraněn.", duration = SnackbarDuration.Long)
                            }
                        }
                    },
                    onReset = {
                        // Smazani cele tabulky na pozadi
                        scope.launch(Dispatchers.IO) {
                            dao.deleteAll()
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar("Všechna data smazána.", duration = SnackbarDuration.Long)
                                onReset()
                            }
                        }
                    }
                )
            }
        }
    }
}