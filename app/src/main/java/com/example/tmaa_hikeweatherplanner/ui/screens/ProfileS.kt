package com.example.tmaa_hikeweatherplanner.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.data.SavedLocation

@Composable
fun ProfileS(userName: String, userDate: String, locations: List<SavedLocation>, onBack: () -> Unit, onDeleteLocation: (SavedLocation) -> Unit, onReset: () -> Unit) {
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_all_data_title)) },
            confirmButton = { TextButton(onClick = { showConfirmDialog = false; onReset() }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // VLEVO: PROFIL
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileInfo(userName, userDate) { showConfirmDialog = true }
                }
                // VPRAVO: VÝLETY
                Column(modifier = Modifier.weight(1f)) {
                    TripsSection(locations, onDeleteLocation)
                }
            }
        } else {
            ProfileInfo(userName, userDate) { showConfirmDialog = true }
            Spacer(Modifier.height(24.dp))
            TripsSection(locations, onDeleteLocation)
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.back)) }
    }
}

@Composable
fun ProfileInfo(name: String, date: String, onDelete: () -> Unit) {
    Text(stringResource(R.string.my_profile), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.pilgrim_name) + " $name")
    Text(stringResource(R.string.born_date) + " $date")
    OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete_all_data_btn)) }
}

@Composable
fun TripsSection(locs: List<SavedLocation>, onDelete: (SavedLocation) -> Unit) {
    Text(stringResource(R.string.my_trips), style = MaterialTheme.typography.titleLarge)
    locs.forEach { loc ->
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) { Text(loc.city); Text(loc.difficulty, style = MaterialTheme.typography.bodySmall) }
                IconButton(onClick = { onDelete(loc) }) { Text("X", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}