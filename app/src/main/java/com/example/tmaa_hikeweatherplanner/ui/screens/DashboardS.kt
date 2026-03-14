package com.example.tmaa_hikeweatherplanner.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tmaa_hikeweatherplanner.BuildConfig
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tmaa_hikeweatherplanner.network.WeatherApiService

// Dashboard
@Composable
fun DashboardS(
    // Jmeno uzivatele pro uvitaci zpravu
    userName: String,
    // Seznam nedavno hledanych lokaci max 2
    recentLocations: List<SavedLocation>
) {
    // Kontextu pro praci se systemovymi sluzbami (poloha...)
    val context = LocalContext.current

    // Spousteni uloh na pozadi na teto obrazovce
    val scope = rememberCoroutineScope()

    // Aktualni konfigurace zarizeni
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Inicializace komunikace s API pro zjisteni pocasi
    val apiService = remember { WeatherApiService.create() }

    // Uchovani textove adresy aktualni polohy
    var currentAddress by remember { mutableStateOf("Vyhledávám...") }

    // Uchovani informaci o aktualnim pocasi
    var weatherInfo by remember { mutableStateOf("Načítám počasí...") }

    // Ovladani a pamatovani si pozice kamery na mape
    val cameraPositionState = rememberCameraPositionState()

    // Jestli ma aplikace povoleni k pristupu k GPS
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Kod ktery se spousti vzdy kdyz se zmeni opravneni k poloze
    LaunchedEffect(hasLocationPermission) {
        // Pokud ma opravneni zjistuje data
        if (hasLocationPermission) {
            getCurrentLocation(context) { location ->
                // Pokud se podarilo ziskat polohu
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    // Posunuti kamery na mape na zjistenou polohu
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13f)

                    // Prelozeni souradnic na pozadi na textovou adresu
                    fetchDetailedAddress(context, latLng) { address ->
                        currentAddress = address
                    }

                    // Spusteni stahovani pocasi v coroutine
                    scope.launch {
                        try {
                            val response = apiService.getCurrentWeather(
                                lat = location.latitude,
                                lon = location.longitude,
                                apiKey = BuildConfig.WEATHER_API_KEY
                            )
                            val temp = response.main.temp.toInt()
                            val desc = response.weather.firstOrNull()?.description ?: ""

                            // Zformatovani pocasi do textu
                            weatherInfo = "Aktuálně: $temp°C, ${desc.replaceFirstChar { it.uppercase() }}"
                        } catch (e: Exception) {
                            weatherInfo = "Počasí momentálně nedostupné"
                        }
                    }
                } else {
                    currentAddress = "Poloha nedostupná"
                    weatherInfo = "Nelze zjistit počasí"
                }
            }
        } else {
            // Informovani pokud nemame povoleni k poloze
            currentAddress = "Nepovoleno"
            weatherInfo = "Chybí oprávnění pro GPS"
        }
    }

    // Vyvolani dialogu s zadosti o povoleni GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Aktualizovani stavu podle toho jestli uzivatel povolil opravneni
        hasLocationPermission = permissions.values.any { it }
    }

    // Rolovaci seznam cele obrazovky
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // Odsazeni list pres Scaffold v MainApp
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Uvitaci text se jmenem uzivatele
        item {
            Text(
                text = stringResource(id = R.string.greeting) + " $userName!",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Karta zobrazujici mapu a aktualni pocasi
        item {
            WeatherCard(hasLocationPermission, currentAddress, weatherInfo, cameraPositionState) {
                // Po kliknuti na tlacitko zadost o opravneni
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        // Zobrazeni hledani pokud jsou v historii
        if (recentLocations.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.recent_searches),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // Seznam lokaci
            items(
                items = recentLocations,
                key = { "${it.latitude}_${it.longitude}" }
            ) { loc ->
                HistoryCard(loc, modifier = Modifier.animateItem())
            }
        }
    }
}

// Zobrazeni jednoho ulozeneho vyletu v historii na Dashboardu
@Composable
fun HistoryCard(loc: SavedLocation, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(loc.city, style = MaterialTheme.typography.titleMedium)
                    Text(loc.difficulty, style = MaterialTheme.typography.bodySmall)
                }
                Text(loc.weather, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = "GPS: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Funkce pro ziskani jednorazove aktualni GPS polohy
@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onResult: (android.location.Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    // Vyzadani polohy s vysokou presnosti
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { location -> onResult(location) }
        .addOnFailureListener { onResult(null) }
}

// Preklad GPS souradnic na textovou adresu bezici na pozadi
private fun fetchDetailedAddress(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
    // Spusteni v jinem vlakne aby se nezasekla aplikace
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val addr = addresses?.firstOrNull()

            val resultText = if (addr != null) {
                val fullAddress = addr.getAddressLine(0)
                // Orezani zbytecnych informaci adresy
                fullAddress
                    .replace(", Česko", "")
                    .replace(", Czechia", "")
                    .replace(Regex("\\d{3}\\s?\\d{2}\\s"), "")
            } else {
                "Neznámá lokalita"
            }

            // Vraceni vysledku zpet do hlavniho vlakna pro vykresleni v UI
            withContext(Dispatchers.Main) {
                onResult(resultText)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Chyba zjišťování")
            }
        }
    }
}

// Komponenta zobrazujici kartu s mapou a informacemi o aktualnim pocasi v miste uzivatele
@Composable
fun WeatherCard(
    hasLocationPermission: Boolean,
    address: String,
    weather: String,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onPermissionRequest: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hlavicka karty s ikonkou a adresou
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Moje poloha: $address",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))

            // Zobrazeni mapy od Googlu
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    // Povoleni modre tecky polohy pokud je opravneni
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    // Skryti tlacitek pro priblizovani, ponechani tlacitka pro zamereni polohy
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                )
            }
            Spacer(Modifier.height(12.dp))

            // Paticka karty s informacemi o pocasi
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null)
                Spacer(Modifier.width(8.dp))
                Text(text = weather, style = MaterialTheme.typography.bodyLarge)
            }

            // Pokud aplikace nema pravo zjistovat polohu, zobrazi tlacitko pro jeho vyzadani
            if (!hasLocationPermission) {
                Button(onClick = onPermissionRequest, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Povolit GPS")
                }
            }
        }
    }
}