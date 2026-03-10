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
    //vytvoreni ovladace pro navigaci mezi obrazovkama
    val navController = rememberNavController()
    //ulozeni jmena uzivatele
    var userName by rememberSaveable { mutableStateOf<String?>(null) }
    //ulozeni data narozeni
    var userBirthDate by rememberSaveable { mutableStateOf("") }

    //seznam vsech ulozenych lokaci
    val savedLocations = remember { mutableStateListOf<SavedLocation>() }
    //Seznam pro historii hledani(pro vypis polsednich 3)
    val recentLocations = remember { mutableStateListOf<SavedLocation>() }

    NavHost(
        navController = navController,
        //pokud neni jeste jsmeno zobrazi se setup, jinak dashboard
        startDestination = if (userName == null) InitialRoute else DashboardRoute,
        modifier = modifier
    ) {
        //routa pro uvodni nastavovaci obrazovku
        composable<InitialRoute> {
            InitialS(onSetupComplete = { name, date ->
                userName = name
                userBirthDate = date
                //presun na dashboard
                navController.navigate(DashboardRoute) {
                    popUpTo(InitialRoute) { inclusive = true }
                }
            })
        }
        //routa pro hlavni obrazovku
        composable<DashboardRoute> {
            DashboardS(
                userName = userName ?: "",
                recentLocations = recentLocations.take(3), //Pouze 3
                onNavigateSearch = { navController.navigate(SearchRoute) },
                onNavigateProfile = { navController.navigate(ProfileRoute) }
            )
        }
        //routa pro vyhledavani
        composable<SearchRoute> {
            SearchS(
                recentLocations = recentLocations.take(3),
                onBack = { navController.popBackStack() },
                onLocationSaved = { city, diff, temp ->
                    val newLoc = SavedLocation(city, diff, temp)
                    savedLocations.add(newLoc)
                    //Pridani na zacatek seznamu minulych vyhledav.
                    recentLocations.add(0, newLoc)
                    //Pokud v historii vic jak 3 nejstarsi odstrani
                    if (recentLocations.size > 3) {
                        recentLocations.removeAt(3)
                    }
                }
            )
        }
        //routa pro profil uzivatele
        composable<ProfileRoute> {
            ProfileS(
                userName = userName ?: "",
                userDate = userBirthDate,
                locations = savedLocations,
                onBack = { navController.popBackStack() },
                onDeleteLocation = { loc -> savedLocations.remove(loc) },
                onReset = {
                    userName = null
                    savedLocations.clear()
                    recentLocations.clear()
                    navController.navigate(InitialRoute) {
                        popUpTo(DashboardRoute) { inclusive = true }
                    }
                }
            )
        }
    }
}