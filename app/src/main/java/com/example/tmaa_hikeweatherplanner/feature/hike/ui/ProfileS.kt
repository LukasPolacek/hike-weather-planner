package com.example.tmaa_hikeweatherplanner.feature.hike.ui

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
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.SavedLocation
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.UserProfile
import com.example.tmaa_hikeweatherplanner.core.services.LocationService
import java.util.Locale

/**
 * obrazovka profilu
 * zobrazuje informace o uzivateli, umoznuje zapinat/vypinat sluzbu sledovani pocasi na pozadi
 * obsahuje historii ulozenych vyletu s moznosti jejich mazani
 */
@Composable
fun ProfileS(
    userProfile: UserProfile,
    // seznam vsech vyletu primo z databaze stateflow z viewModelu
    locations: List<SavedLocation>,
    // Funkce volana pri kliknuti na smazani konkretniho vyletu
    onDeleteLocation: (SavedLocation) -> Unit,
    // Funkce volana pro uplne smazani vsech dat aplikace
    onReset: () -> Unit
) {
    // kontext pro spousteni sluzeb
    val context = LocalContext.current

    // Stav urcujici, zda se ma zobrazit varovny dialog pred smazanim dat
    // remembersaveable aby dialog nezmizel pri otoceni telefonu
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // Zjisteni orientace pro pripadne rozdeleni obrazovky na dva sloupce
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Vyzadani opravneni pro zasilani notifikaci
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Efekt ktery se spusti pri prvnim nacteni teto obrazovky
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
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileInfo(userProfile) { showConfirmDialog = true }
                Spacer(Modifier.height(8.dp))
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
                ProfileInfo(userProfile, modifier = Modifier.fillMaxWidth()) { showConfirmDialog = true }
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

// Stateless komponenta ktera neudrzuje zadna data ale
// pouze zobrazi co dostane v parametrech
@Composable
fun ProfileInfo(userProfile: UserProfile, modifier: Modifier = Modifier, onDelete: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zmenseny nadpis z headlineMedium na titleLarge pro usporu mista
        Text(stringResource(id = R.string.my_profile), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp)) // Zmensena mezera
        Text(stringResource(id = R.string.pilgrim_name) + " ${userProfile.name}", style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(id = R.string.born_date) + " ${userProfile.birthDate}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp)) // Zmensena mezera pred tlacitkem

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
        // Zmensen padding z 16.dp na 12.dp
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Zmensen nadpis z titleMedium na titleSmall
            Text("Sledování výletu (pozadí)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Spusteni startem jako foreground aby ji system neukoncil
                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java)
                        // startforegroundservice aby system nezabil a ukazal notifikaci
                        context.startForegroundService(intent)
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
            // Zobrazeni GPS souradnic na 4 desetinna mista s Locale.US pro univerzalni zapis
            Text(
                text = "GPS: ${String.format(Locale.US, "%.4f", loc.latitude)}, ${String.format(Locale.US, "%.4f", loc.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}