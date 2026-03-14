package com.example.tmaa_hikeweatherplanner.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tmaa_hikeweatherplanner.ui.MainApp
import com.example.tmaa_hikeweatherplanner.ui.screens.InitialS

@Composable
fun HikeNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Držíme pouze základní stav uživatele pro rozhodnutí, kam navigovat
    var userName by rememberSaveable { mutableStateOf<String?>(null) }
    var userBirthDate by rememberSaveable { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = if (userName == null) "initial" else "main",
        modifier = modifier
    ) {
        // 1. Úvodní obrazovka (Formulář)
        composable("initial") {
            InitialS(onSetupComplete = { name, date ->
                userName = name
                userBirthDate = date
                // Přechod na hlavní aplikaci a smazání úvodní obrazovky z historie
                navController.navigate("main") { popUpTo("initial") { inclusive = true } }
            })
        }

        // 2. Hlavní kontejner aplikace se spodní lištou (Zahrnuje Dashboard, Search i Profile)
        composable("main") {
            MainApp(
                userName = userName ?: "",
                userDate = userBirthDate,
                onReset = {
                    userName = null
                    // Návrat na úvodní obrazovku a vymazání historie hlavní aplikace
                    navController.navigate("initial") { popUpTo("main") { inclusive = true } }
                }
            )
        }
    }
}