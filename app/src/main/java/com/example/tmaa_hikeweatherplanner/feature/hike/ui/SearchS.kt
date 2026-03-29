package com.example.tmaa_hikeweatherplanner.feature.hike.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tmaa_hikeweatherplanner.BuildConfig
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.SavedLocation
import com.example.tmaa_hikeweatherplanner.core.network.WeatherApiService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Hledat
 * Umoznuje vyhledat libovolne misto napsanim textu nebo posunem znacky na mape
 * Potom stahne aktualni pocasi pro lokaci a umozni uzivateli vylet ulozit do databaze
 */
@Composable
fun SearchS(
    // historie poslednich hledani, z mainApp
    recentLocations: List<SavedLocation>,
    // callback funkce pro odeslani finalnich dat zpet do mainapp a do viewmodelu
    onLocationSaved: (String, String, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // zjisteni orientace pro pripadne dvousloupcove rozlozeni na sirku
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // instance pro stahovani pocasi
    val apiService = remember { WeatherApiService.create() }

    // stavy pro uchovani dat o hledanem miste
    var cityQuery by rememberSaveable { mutableStateOf("") }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var fetchedWeather by remember { mutableStateOf("Nezjištěno") }

    // moznosti narocnosti pro rozbalovaci menu
    val options = listOf("Lehká", "Střední", "Náročná")
    var selectedDiff by rememberSaveable { mutableStateOf(options[0]) }

    // inicializace mapy, vychozi praha zoom 10
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(50.0755, 14.4378), 10f)
    }

    val scrollState = rememberScrollState()

    // pomocna funkce ktera se zavola po kliknuti na tlacitko lupy v klavesnici
    val performSearch = {
        // ziskani GPS souradnic z textu pomoci geocoderu asynchorne
        searchCityOnMap(context, cityQuery) { coords ->
            selectedLatLng = coords
            // prelet kamery na mape na nalezene souradnice
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(coords, 15f)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = stringResource(id = R.string.search_other_place),
            style = if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 24.dp))

        // rozlozeni obsahu na zaklade orientace displeje
        if (isLandscape) {
            // na sirku - dvousloupcove rozlozeni
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                // vlevo mapa + tlacitko
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Zpřesněte polohu na mapě:", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))

                    MapPickerView(cameraPositionState, mapHeight = 140.dp)

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            val latLng = cameraPositionState.position.target
                            fetchAddressFromCoords(context, latLng) { address ->
                                cityQuery = address
                                selectedLatLng = latLng
                                scope.launch {
                                    fetchedWeather = fetchRealWeather(apiService, latLng, BuildConfig.WEATHER_API_KEY)
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Načíst adresu pod značkou")
                    }
                }

                // vpravo - hledani + karta
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        label = { Text(stringResource(id = R.string.enter_city)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                        trailingIcon = {
                            IconButton(onClick = { performSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = "Hledat")
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.weight(1f))

                    SaveLocationCard(cityQuery, fetchedWeather, selectedDiff, options, { selectedDiff = it }) {
                        selectedLatLng?.let { coords ->
                            onLocationSaved(cityQuery, selectedDiff, fetchedWeather, coords.latitude, coords.longitude)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // historie  pres celou sirku displeje
            HistorySection(recentLocations)

        } else {
            // rezim na vysku - vse pod sebou
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
    }
}

/**
 * funkce pro stazeni pocasi z openweather API
 * vraci naformatovany text pro zobrazeni v ui
 */
private suspend fun fetchRealWeather(api: WeatherApiService, coords: LatLng, key: String): String {
    return try {
        val res = api.getCurrentWeather(coords.latitude, coords.longitude, key)
        "${res.main.temp.toInt()}°C, ${res.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""}"
    } catch (e: Exception) {
        "Počasí nedostupné"
    }
}

/**
 * zobrazeni boxu s Google Mapou a cervenou ikonou  uprostred
 */
@Composable
fun MapPickerView(cameraState: CameraPositionState, mapHeight: Dp = 250.dp) {
    Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
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
}

/**
 * komponenta zobrazujici box s mapou vcetne tlacitka pro nacteni adresy v rezimu na vysku
 */
@Composable
fun MapPickerSection(context: Context, cameraState: CameraPositionState, onDataFound: (String, LatLng) -> Unit) {
    Column {
        Text("Zpřesněte polohu na mapě:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        MapPickerView(cameraState, mapHeight = 250.dp)
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

/**
 * karta kde se zobrazi obsah az uzivatel vyhleda nejakou lokaci.
 * Udelana kompaktnejsi, aby se lepe vesla na sirku.
 */
@Composable
fun SaveLocationCard(q: String, weather: String, diff: String, opts: List<String>, sDi: (String)->Unit, onSave: ()->Unit) {
    var ex by remember { mutableStateOf(false) }

    if (q.isNotBlank()) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            // Zmenseny padding z 16.dp na 12.dp pro vetsi kompaktnost
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Vybraná lokalita: $q", style = MaterialTheme.typography.titleSmall)
                Text("Zjištěné počasí: $weather", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(8.dp))
                // dropdown menu pro vyber narocnosti vyletu
                Box {
                    OutlinedButton(onClick = { ex = true }, modifier = Modifier.fillMaxWidth()) { Text("Náročnost: $diff") }
                    DropdownMenu(expanded = ex, onDismissRequest = { ex = false }) {
                        opts.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { sDi(it); ex = false }) }
                    }
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(id = R.string.save_to_list))
                }
            }
        }
    }
}

/**
 * preklad textove adresy na gps souradnice, prime geocoding
 * asynchronne, IO vlakno
 */
private fun searchCityOnMap(context: Context, query: String, onResult: (LatLng) -> Unit) {
    if (query.isBlank()) return
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(query, 1)
            results?.firstOrNull()?.let {
                withContext(Dispatchers.Main) {
                    onResult(LatLng(it.latitude, it.longitude))
                }
            }
        } catch (e: Exception) { }
    }
}

/**
 * preklad GPS souradnic zpet na textovou adresu .zpetne Geocoding
 * asynchronne, IO vlakno
 */
private fun fetchAddressFromCoords(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val addr = addresses?.firstOrNull()

            val resultText = if (addr != null) {
                val fullAddress = addr.getAddressLine(0)
                // odstraneni zbytecnych informaci z adresy
                fullAddress
                    .replace(", Česko", "")
                    .replace(", Czechia", "")
                    .replace(Regex("\\d{3}\\s?\\d{2}\\s"), "")
            } else {
                // pokud geocoder nefunguje, treba kdyz je tecka na mori vrati aspon souradnice
                "Souřadnice: ${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)}"
            }

            withContext(Dispatchers.Main) {
                onResult(resultText)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Chyba vyhledávání")
            }
        }
    }
}

/**
 * zobrazeni kratkeho seznamu historie hledani
 */
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