package com.example.tmaa_hikeweatherplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
//hlavni obrazovka s prehledem
@Composable
fun DashboardS(
    userName: String,
    recentLocations: List<SavedLocation>,
    onNavigateSearch: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    //scroll pro rezim na sirku
    val scrollState = rememberScrollState()
    //hlavni kontejner s podporou posunu obsahu
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //uvitaci text
        Text("Ahoj, $userName!", style = MaterialTheme.typography.headlineMedium)

        // Hlavní karta s polohou a pocasim
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📍 Moje poloha: Vyhledávám...", style = MaterialTheme.typography.titleMedium)
                Text("☁️ Aktuální počasí: 12°C, Polojasno")
            }
        }
        //button pro vyhledavani
        Button(onClick = onNavigateSearch, modifier = Modifier.fillMaxWidth()) {
            Text("Hledat jiné místo")
        }

        Spacer(modifier = Modifier.height(12.dp))

        //button pro profil
        FilledTonalButton(onClick = onNavigateProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Můj profil a seznam")
        }
        //zobrazeni historie kdyz nemame prazdny seznam
        if (recentLocations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Poslední hledání", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            //vypis jednotlivych historickychhledani
            recentLocations.forEach { loc ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loc.city, style = MaterialTheme.typography.titleMedium)
                            Text("Náročnost: ${loc.difficulty}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(loc.weather)
                    }
                }
            }
        }
    }
}