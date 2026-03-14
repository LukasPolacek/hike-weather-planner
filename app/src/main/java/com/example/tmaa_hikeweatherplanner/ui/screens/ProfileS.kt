package com.example.tmaa_hikeweatherplanner.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
import com.example.tmaa_hikeweatherplanner.services.LocationService

// Obrazovka profilu s informacema o uzivateli, ovladani sluzby na pozadi a historii vsech vyletu
@Composable
fun ProfileS(
    // Jmeno uzivatele pri prvnim spusteni
    userName: String,
    // Datum narozeni uzivatele
    userDate: String,
    // Kompletni seznam vsech ulozenych lokaci
    locations: List<SavedLocation>,
    // Funkce volana pri kliknuti na smazani konkretniho vyletu
    onDeleteLocation: (SavedLocation) -> Unit,
    // Funkce volana pro uplne smazani vsech dat aplikace
    onReset: () -> Unit
) {
    // Ziskani kontextu aplikace
    val context = LocalContext.current

    // Stavova promenna urcujici, zda se ma zobrazit varovny dialog pred smazanim dat
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // Detekce orientace zarizeni na vysku/na sirku
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Vyzadani opravneni pro zasilani notifikaci
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Efekt, ktery se spusti pri prvnim nacteni teto obrazovky
    LaunchedEffect(Unit) {
        // explicitni souhlas pro zasilani notifikaci
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Potvrzovaci dialog kdyz chce uzivatel smazat vsechny data
    if (showConfirmDialog) {
        AlertDialog(
            // Zavreni dialogu pri kliknuti mimo nej
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(id = R.string.delete_all_data_title)) },
            text = { Text(stringResource(id = R.string.delete_all_data_text)) },
            // Tlacitko pro potvrzeni smazani
            confirmButton = {
                TextButton(onClick = { showConfirmDialog = false; onReset() }) {
                    Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            // Tlacitko pro zruseni akce
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // Rezim na sirku prvky vedle sebe
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Levy sloupec pro informace o profilu a prepinac sluzby
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileInfo(userName, userDate) { showConfirmDialog = true }
                Spacer(Modifier.height(16.dp))
                TrackingCard(context)
            }
            // Pravy sloupec pro seznam vyletu
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(stringResource(id = R.string.my_trips), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                }
                // Jednotlive vylety
                items(
                    items = locations,
                    key = { "${it.latitude}_${it.longitude}" }
                ) { loc ->
                    TripCard(loc, onDeleteLocation, Modifier.animateItem())
                }
            }
        }
    } else {
        // Rezim na vysku vse pod sebou
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileInfo(userName, userDate, modifier = Modifier.fillMaxWidth()) { showConfirmDialog = true }
            }
            item {
                TrackingCard(context, modifier = Modifier.fillMaxWidth())
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(id = R.string.my_trips), style = MaterialTheme.typography.titleLarge)
            }
            // Jednotlive vylety
            items(
                items = locations,
                key = { "${it.latitude}_${it.longitude}" }
            ) { loc ->
                TripCard(loc, onDeleteLocation, Modifier.animateItem())
            }
        }
    }
}

// Stateless) komponenta pro jmeno a datum narozeni
@Composable
fun ProfileInfo(name: String, date: String, modifier: Modifier = Modifier, onDelete: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(id = R.string.my_profile), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(id = R.string.pilgrim_name) + " $name", style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(id = R.string.born_date) + " $date", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))

        // Tlacitko pro proces smazani vsech dat
        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(id = R.string.delete_all_data_btn))
        }
    }
}

// Komponenta zajistujici start a stop sledovani GPS
@Composable
fun TrackingCard(context: Context, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sledování výletu (pozadí)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Spusteni startem jako foreground aby ji system neukoncil
                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java)
                        // Spusteni podle verze androidu
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Start") }

                // Ukonceni sluzby stop tlacitkem
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java)
                        context.stopService(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
            }
        }
    }
}

// Vykresleni konkretniho ulozeneho vyletu s detailem o pocasi a poloze
@Composable
fun TripCard(loc: SavedLocation, onDelete: (SavedLocation) -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Udaje o meste a zvolene narocnosti
                Column(modifier = Modifier.weight(1f)) {
                    Text(loc.city, style = MaterialTheme.typography.titleMedium)
                    Text(loc.difficulty, style = MaterialTheme.typography.bodySmall)
                }
                // Posledni zjistene pocasi v dane lokaci
                Text(loc.weather, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))

                // Tlacitko pro odstraneni vyletu ze seznamu
                IconButton(onClick = { onDelete(loc) }) {
                    Text("X", color = MaterialTheme.colorScheme.error)
                }
            }
            // Zobrazeni GPS souradnic na 4 desetinna mista
            Text(
                text = "GPS: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}