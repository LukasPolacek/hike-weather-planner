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

@Composable
fun DashboardS(
    userName: String,
    recentLocations: List<SavedLocation>,
    onNavigateSearch: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    //detekovani jestli je to v rezimu na sirku
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
        //nadpis nahore
        Text(
            text = stringResource(id = R.string.greeting) + " $userName!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLandscape) {
            //rozlozeni na landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                //vlevo pocasi
                Column(modifier = Modifier.weight(1f)) {
                    WeatherCard(hasLocationPermission) {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                }
                //Vpravo tlacitka a historie
                Column(modifier = Modifier.weight(1f)) {
                    DashboardButtons(onNavigateSearch, onNavigateProfile)
                    RecentHistory(recentLocations)
                }
            }
        } else {
            //klasicke rozlozeni na vysku
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

// Pomocná komponenta pro kartu počasí, abychom neopakovali kód
@Composable
fun WeatherCard(hasLocationPermission: Boolean, onPermissionRequest: () -> Unit) {
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

            // --- PLACEHOLDER PRO MAPU ---
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Zde bude mapa (Seminář 7/8)", style = MaterialTheme.typography.bodySmall)
                }
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

// Pomocná komponenta pro tlačítka
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

// Pomocná komponenta pro historii
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