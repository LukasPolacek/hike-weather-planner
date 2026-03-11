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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.launch
import com.example.tmaa_hikeweatherplanner.network.WeatherApiService

@Composable
fun DashboardS(
    userName: String,
    recentLocations: List<SavedLocation>,
    onNavigateSearch: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val apiService = remember { WeatherApiService.create() }

    var currentAddress by remember { mutableStateOf("Vyhledávám...") }
    var weatherInfo by remember { mutableStateOf("Načítám počasí...") }
    val cameraPositionState = rememberCameraPositionState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            getCurrentLocation(context) { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13f)

                    fetchDetailedAddress(context, latLng) { address ->
                        currentAddress = address
                    }

                    scope.launch {
                        try {
                            val response = apiService.getCurrentWeather(
                                lat = location.latitude,
                                lon = location.longitude,
                                apiKey = BuildConfig.WEATHER_API_KEY
                            )
                            val temp = response.main.temp.toInt()
                            val desc = response.weather.firstOrNull()?.description ?: ""
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
            currentAddress = "Nepovoleno"
            weatherInfo = "Chybí oprávnění pro GPS"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.greeting) + " $userName!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    WeatherCard(hasLocationPermission, currentAddress, weatherInfo, cameraPositionState) {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    DashboardButtons(onNavigateSearch, onNavigateProfile)
                    RecentHistory(recentLocations)
                }
            }
        } else {
            WeatherCard(hasLocationPermission, currentAddress, weatherInfo, cameraPositionState) {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            Spacer(modifier = Modifier.height(16.dp))
            DashboardButtons(onNavigateSearch, onNavigateProfile)
            RecentHistory(recentLocations)
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onResult: (android.location.Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { location -> onResult(location) }
        .addOnFailureListener { onResult(null) }
}

private fun fetchDetailedAddress(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
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
            onResult("Neznámá lokalita")
        }
    } catch (e: Exception) {
        onResult("Chyba zjišťování")
    }
}

@Composable
fun WeatherCard(
    hasLocationPermission: Boolean,
    address: String,
    weather: String,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onPermissionRequest: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Moje poloha: $address",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null)
                Spacer(Modifier.width(8.dp))
                Text(text = weather, style = MaterialTheme.typography.bodyLarge)
            }
            if (!hasLocationPermission) {
                Button(onClick = onPermissionRequest, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Povolit GPS")
                }
            }
        }
    }
}

@Composable
fun DashboardButtons(onSearch: () -> Unit, onProfile: () -> Unit) {
    Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Search, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.search_other_place))
    }
    Spacer(modifier = Modifier.height(8.dp))
    FilledTonalButton(onClick = onProfile, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Person, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.my_profile_and_list))
    }
}

@Composable
fun RecentHistory(recentLocations: List<SavedLocation>) {
    if (recentLocations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.recent_searches), style = MaterialTheme.typography.titleLarge)
        recentLocations.forEach { loc ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loc.city, style = MaterialTheme.typography.titleMedium)
                            Text(loc.difficulty, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(loc.weather, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("GPS: ${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}