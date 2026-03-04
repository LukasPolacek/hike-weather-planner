package com.example.tmaa_hikeweatherplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.data.SavedLocation

//obrazovka profilu a seznamu vyletu
@Composable
fun ProfileS(
    userName: String,
    userDate: String,
    locations: List<SavedLocation>,
    onBack: () -> Unit,
    onDeleteLocation: (SavedLocation) -> Unit,
    onReset: () -> Unit
) {
    //stav pro potvrzovaci dialog
    var showConfirmDialog by remember { mutableStateOf(false) }
    //stav pro plynuly posun obsahu
    val scrollState = rememberScrollState()

    //vyskakovaci okno pro potvrzeni smazani
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Smazat všechna data?") },
            text = { Text("Opravdu chcete smazat profil i seznam lokací?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onReset()
                }) {
                    Text("Smazat", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //vypis udaju o uzivateli
        Text("Můj Profil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Poutník: $userName")
        Text("Narozen: $userDate")

        //tlacitko ktere meni stav pro zobrazeni dialogu
        OutlinedButton(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.padding(top = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Smazat všechna data")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Moje výlety", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        //seznam ulozenych mist vykresleny v kartach
        locations.forEach { loc ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(loc.city, style = MaterialTheme.typography.titleMedium)
                        Text("Náročnost: ${loc.difficulty}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(loc.weather, modifier = Modifier.padding(end = 8.dp))
                    //tlacitko pro smazani konkretni polozky
                    IconButton(onClick = { onDeleteLocation(loc) }) {
                        Text("X", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zálohovat na Cloud")
        }

        TextButton(onClick = onBack) {
            Text("Zpět")
        }
    }
}