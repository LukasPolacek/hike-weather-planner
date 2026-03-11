package com.example.tmaa_hikeweatherplanner.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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

@Composable
fun ProfileS(
    userName: String,
    userDate: String,
    locations: List<SavedLocation>,
    onBack: () -> Unit,
    onDeleteLocation: (SavedLocation) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(id = R.string.delete_all_data_title)) },
            text = { Text(stringResource(id = R.string.delete_all_data_text)) },
            confirmButton = {
                TextButton(onClick = { showConfirmDialog = false; onReset() }) {
                    Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileInfo(userName, userDate) { showConfirmDialog = true }
                    Spacer(Modifier.height(16.dp))
                    TrackingCard(context)
                }
                Column(modifier = Modifier.weight(1f)) {
                    TripsSection(locations, onDeleteLocation)
                }
            }
        } else {
            ProfileInfo(userName, userDate, modifier = Modifier.fillMaxWidth()) { showConfirmDialog = true }
            Spacer(Modifier.height(16.dp))
            TrackingCard(context, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
            TripsSection(locations, onDeleteLocation)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(id = R.string.back))
        }
    }
}

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
        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(id = R.string.delete_all_data_btn))
        }
    }
}

@Composable
fun TrackingCard(context: Context, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sledování výletu (pozadí)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(context, LocationService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Start") }

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

@Composable
fun TripsSection(locs: List<SavedLocation>, onDelete: (SavedLocation) -> Unit) {
    Text(stringResource(id = R.string.my_trips), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    locs.forEach { loc ->
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(loc.city, style = MaterialTheme.typography.titleMedium)
                        Text(loc.difficulty, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(loc.weather, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { onDelete(loc) }) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(
                    text = "GPS: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}