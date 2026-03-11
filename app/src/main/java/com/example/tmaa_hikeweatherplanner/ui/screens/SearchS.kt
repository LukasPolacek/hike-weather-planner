package com.example.tmaa_hikeweatherplanner.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.BuildConfig
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
import com.example.tmaa_hikeweatherplanner.network.WeatherApiService
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun SearchS(
    recentLocations: List<SavedLocation>,
    onBack: () -> Unit,
    onLocationSaved: (String, String, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val apiService = remember { WeatherApiService.create() }

    var cityQuery by rememberSaveable { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var fetchedWeather by remember { mutableStateOf("Nezjištěno") }

    val options = listOf("Lehká", "Střední", "Náročná")
    var selectedDiff by rememberSaveable { mutableStateOf(options[0]) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(50.0755, 14.4378), 10f)
    }

    val scrollState = rememberScrollState()

    val performSearch = {
        searchCityOnMap(context, cityQuery) { coords ->
            selectedLatLng = coords
            scope.launch {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(coords, 15f)
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Text(stringResource(id = R.string.search_other_place), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cityQuery,
                onValueChange = { cityQuery = it },
                label = { Text(stringResource(id = R.string.enter_city)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                trailingIcon = {
                    IconButton(onClick = { performSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Hledat")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1.2f)) {
                    MapPickerSection(context, cameraPositionState) { name, coords ->
                        cityQuery = name
                        selectedLatLng = coords
                        scope.launch {
                            fetchedWeather = fetchRealWeather(apiService, coords, BuildConfig.WEATHER_API_KEY)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SaveLocationCard(cityQuery, fetchedWeather, selectedDiff, options, { selectedDiff = it }) {
                        selectedLatLng?.let { coords ->
                            onLocationSaved(cityQuery, selectedDiff, fetchedWeather, coords.latitude, coords.longitude)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    HistorySection(recentLocations)
                }
            }
        } else {
            MapPickerSection(context, cameraPositionState) { name, coords ->
                cityQuery = name
                selectedLatLng = coords
                scope.launch {
                    fetchedWeather = fetchRealWeather(apiService, coords, BuildConfig.WEATHER_API_KEY)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            SaveLocationCard(cityQuery, fetchedWeather, selectedDiff, options, { selectedDiff = it }) {
                selectedLatLng?.let { coords ->
                    onLocationSaved(cityQuery, selectedDiff, fetchedWeather, coords.latitude, coords.longitude)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            HistorySection(recentLocations)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(id = R.string.done_back_dashboard))
        }
    }
}

private suspend fun fetchRealWeather(api: WeatherApiService, coords: LatLng, key: String): String {
    return try {
        val res = api.getCurrentWeather(coords.latitude, coords.longitude, key)
        "${res.main.temp.toInt()}°C, ${res.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""}"
    } catch (e: Exception) {
        "Počasí nedostupné"
    }
}

@Composable
fun MapPickerSection(context: Context, cameraState: com.google.maps.android.compose.CameraPositionState, onDataFound: (String, LatLng) -> Unit) {
    Column {
        Text("Zpřesněte polohu na mapě:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            )
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(36.dp).offset(y = (-18).dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {
                val latLng = cameraState.position.target
                fetchAddressFromCoords(context, latLng) { address ->
                    onDataFound(address, latLng)
                }
            },
            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
        ) {
            Text("Načíst adresu pod značkou")
        }
    }
}

@Composable
fun SaveLocationCard(q: String, weather: String, diff: String, opts: List<String>, sDi: (String)->Unit, onSave: ()->Unit) {
    var ex by remember { mutableStateOf(false) }

    if (q.isNotBlank()) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Vybraná lokalita: $q", style = MaterialTheme.typography.titleMedium)
                Text("Zjištěné počasí: $weather", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(onClick = { ex = true }, modifier = Modifier.fillMaxWidth()) { Text("Náročnost: $diff") }
                    DropdownMenu(expanded = ex, onDismissRequest = { ex = false }) {
                        opts.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { sDi(it); ex = false }) }
                    }
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(stringResource(id = R.string.save_to_list))
                }
            }
        }
    }
}

private fun searchCityOnMap(context: Context, query: String, onResult: (LatLng) -> Unit) {
    if (query.isBlank()) return
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = geocoder.getFromLocationName(query, 1)
        results?.firstOrNull()?.let {
            onResult(LatLng(it.latitude, it.longitude))
        }
    } catch (e: Exception) { }
}

private fun fetchAddressFromCoords(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        val addr = addresses?.firstOrNull()

        if (addr != null) {
            val fullAddress = addr.getAddressLine(0)
            val cleanedAddress = fullAddress
                .replace(", Česko", "")
                .replace(", Czechia", "")
                .replace(Regex("\\d{3}\\s?\\d{2}\\s"), "")

            onResult(cleanedAddress)
        } else {
            onResult("Souřadnice: ${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)}")
        }
    } catch (e: Exception) {
        onResult("Chyba vyhledávání")
    }
}

@Composable
fun HistorySection(recent: List<SavedLocation>) {
    if (recent.isNotEmpty()) {
        Text(stringResource(id = R.string.recently_searched), style = MaterialTheme.typography.titleMedium)
        recent.forEach { loc ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row {
                        Text(loc.city, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text(loc.weather)
                    }
                    Text("GPS: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}