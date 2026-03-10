package com.example.tmaa_hikeweatherplanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.data.SavedLocation
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun DashboardS(
    userName: String,
    recentLocations: List<SavedLocation>,
    onNavigateSearch: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
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
                    WeatherCard(hasLocationPermission) {
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
            WeatherCard(hasLocationPermission) {
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

@Composable
fun WeatherCard(hasLocationPermission: Boolean, onPermissionRequest: () -> Unit) {
    val cameraPositionState = rememberCameraPositionState()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (hasLocationPermission) "Moje poloha: Zjištěna" else stringResource(R.string.location_searching),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            //mapa
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission //pin na mape
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false
                    )
                )
            }
            // ----------------------------

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.current_weather_placeholder))
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
                Row(modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(loc.city, style = MaterialTheme.typography.titleMedium)
                        Text(loc.difficulty, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(loc.weather)
                }
            }
        }
    }
}