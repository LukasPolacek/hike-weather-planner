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
fun SearchS(recentLocations: List<SavedLocation>, onBack: () -> Unit, onLocationSaved: (String, String, String) -> Unit) {
    var cityQuery by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val options = listOf("Lehká", "Střední", "Náročná")
    var selectedDiff by rememberSaveable { mutableStateOf(options[0]) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Text(stringResource(R.string.search_other_place), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                //vlevo hledani
                Column(modifier = Modifier.weight(1f)) {
                    SearchInputs(cityQuery, {cityQuery=it}, selectedDiff, options, expanded, {expanded=it}, {selectedDiff=it}, onLocationSaved)
                }
                //vpravo historie
                Column(modifier = Modifier.weight(1f)) {
                    HistorySection(recentLocations)
                }
            }
        } else {
            SearchInputs(cityQuery, {cityQuery=it}, selectedDiff, options, expanded, {expanded=it}, {selectedDiff=it}, onLocationSaved)
            HistorySection(recentLocations)
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.done_back_dashboard)) }
    }
}

@Composable
fun SearchInputs(q: String, onQ: (String)->Unit, diff: String, opts: List<String>, ex: Boolean, sEx: (Boolean)->Unit, sDi: (String)->Unit, onSave: (String,String,String)->Unit) {
    OutlinedTextField(value = q, onValueChange = onQ, label = { Text(stringResource(R.string.enter_city)) }, modifier = Modifier.fillMaxWidth())
    if (q.isNotBlank()) {
        ElevatedCard(modifier = Modifier.padding(top = 16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(q, style = MaterialTheme.typography.titleLarge)
                Box {
                    OutlinedButton(onClick = { sEx(true) }) { Text(diff) }
                    DropdownMenu(expanded = ex, onDismissRequest = { sEx(false) }) { opts.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { sDi(it); sEx(false) }) } }
                }
                Button(onClick = { onSave(q, diff, "18°C"); onQ("") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(stringResource(R.string.save_to_list)) }
            }
        }
    }
}

@Composable
fun HistorySection(recent: List<SavedLocation>) {
    if (recent.isNotEmpty()) {
        Text(stringResource(R.string.recently_searched), style = MaterialTheme.typography.titleMedium)
        recent.forEach { loc ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text(loc.city, modifier = Modifier.weight(1f))
                    Text(loc.weather)
                }
            }
        }
    }
}