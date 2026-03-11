package com.example.tmaa_hikeweatherplanner.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tmaa_hikeweatherplanner.ui.screens.*
import com.example.tmaa_hikeweatherplanner.data.SavedLocation

@Composable
fun HikeNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    var userName by rememberSaveable { mutableStateOf<String?>(null) }
    var userBirthDate by rememberSaveable { mutableStateOf("") }
    val savedLocations = remember { mutableStateListOf<SavedLocation>() }
    val recentLocations = remember { mutableStateListOf<SavedLocation>() }

    NavHost(
        navController = navController,
        startDestination = if (userName == null) "initial" else "dashboard",
        modifier = modifier
    ) {
        composable("initial") {
            InitialS(onSetupComplete = { name, date ->
                userName = name
                userBirthDate = date
                navController.navigate("dashboard") { popUpTo("initial") { inclusive = true } }
            })
        }

        composable("dashboard") {
            DashboardS(
                userName = userName ?: "",
                recentLocations = recentLocations,
                onNavigateSearch = { navController.navigate("search") },
                onNavigateProfile = { navController.navigate("profile") }
            )
        }

        composable("search") {
            SearchS(
                recentLocations = recentLocations,
                onBack = { navController.popBackStack() },
                onLocationSaved = { city, diff, temp, lat, lon ->
                    val newLoc = SavedLocation(city, diff, temp, lat, lon)
                    savedLocations.add(0, newLoc)
                    recentLocations.add(0, newLoc)
                    if (recentLocations.size > 3) recentLocations.removeAt(3)
                }
            )
        }

        composable("profile") {
            ProfileS(
                userName = userName ?: "",
                userDate = userBirthDate,
                locations = savedLocations,
                onBack = { navController.popBackStack() },
                onDeleteLocation = { loc ->
                    savedLocations.remove(loc)
                    recentLocations.remove(loc)
                },
                onReset = {
                    userName = null
                    savedLocations.clear()
                    recentLocations.clear()
                    navController.navigate("initial") { popUpTo("dashboard") { inclusive = true } }
                }
            )
        }
    }
}