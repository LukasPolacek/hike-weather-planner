package com.example.tmaa_hikeweatherplanner.feature.hike.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tmaa_hikeweatherplanner.BuildConfig
import com.example.tmaa_hikeweatherplanner.R
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.SavedLocation
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.UserProfile
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
import com.example.tmaa_hikeweatherplanner.core.network.WeatherApiService
import com.google.maps.android.compose.CameraPositionState

/**
 * Uvodni Dashboard
 * Zobrazuje uvitaci zpravu, aktualni polohu uzivatele na mape vcetne pocasi a dve posledni hledane lokace
 */
@Composable
fun DashboardS(
    userProfile: UserProfile,
    // seznam nedavno hledanych lokaci 2 ktery posila mainapp z viewmodelu
    recentLocations: List<SavedLocation>
) {
    // Ziskani kontextu uvnitr Compose pro praci se systemovymi sluzbami poloha, geocoder...
    val context = LocalContext.current

    // vytvoreni Scope pro spousteni asynchronnich operaci ktere se deji na teto obrazovce
    // napr stahovani pocasi
    val scope = rememberCoroutineScope()

    // inicializace instance pro komunikaci s OpenWeather API i pri prekresleni
    val apiService = remember { WeatherApiService.create() }

    // stavy pro uchovani asynchronne ziskanych udaju, kdyz se zmeni, UI se na tech mistech prekresli
    var currentAddress by remember { mutableStateOf("Vyhledávám...") }
    var weatherInfo by remember { mutableStateOf("Načítám počasí...") }

    // stav pro kontrolu chovani kamery v Google Mape priblizeni, posun...
    val cameraPositionState = rememberCameraPositionState()

    // stav ktery drzi informaci jestli uzivatel v systemu povolil cist polohu
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // priprava dialogu pro pripadnou zadost o udeleni prav
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // pokud uzivatel povoli alespon jedno opravneni nastavime stav na true
        hasLocationPermission = permissions.values.any { it }
    }

    // launchedeffect spusti blok kodu pokazde kdyz se zmeni hodnota klice hasLocationPermission
    // slouzi k volani veci treba ziskani polohy, ktere by se jinak poustely pri kazdem prekresleni pixelu UI
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            // zavolani pomocne funkce pro ziskavani presne polohy
            getCurrentLocation(context) { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    // zamereni kamery na mape na zjistenou polohu priblizeni 13
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 13f)

                    // prelozeni souradnic na textovou adresu
                    fetchDetailedAddress(context, latLng) { address ->
                        currentAddress = address // aktualizace stavu - prepsani textu v ui
                    }

                    // spusteni asynchroniho stahovani pocasi
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
                            e.printStackTrace()
                            weatherInfo = "Počasí momentálně nedostupné"
                        }
                    }
                } else {
                    currentAddress = "Poloha nedostupná"
                    weatherInfo = "Nelze zjistit počasí"
                }
            }
        } else {
            // pokud pravo nema informuje o tom v ui
            currentAddress = "Nepovoleno"
            weatherInfo = "Chybí oprávnění pro GPS"
        }
    }

    // lazycolumn - skrolovaci seznam ktery do pameti nacte jen to co je aktualne videt
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // bloky v seznamu se pridavaji pres metodu item {}

        // uvitaci text s vyuzitim datoveho modelu userprofile
        item {
            Text(
                text = stringResource(id = R.string.greeting) + " ${userProfile.name}!",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // karta s mapou a pocasim
        item {
            WeatherCard(hasLocationPermission, currentAddress, weatherInfo, cameraPositionState) {
                // po kliknuti na tlacitko uvnitr karty se spusti dialog o prava
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        // blok s historii vyhledavani, zobrazuje se jen kdyz neni prazdny)
        if (recentLocations.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.recent_searches),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // items {} pro vykresleni seznamu z databaze
            items(
                items = recentLocations,
                // klice pro poznavani polozky pri razeni a mazani
                key = { "${it.latitude}_${it.longitude}" }
            ) { loc ->
                // vykresleni jedne karty pro jeden vylet
                HistoryCard(loc, modifier = Modifier.animateItem())
            }
        }
    }
}

/**
 * Zobrazeni jednoho ulozeneho vyletu v historii na dashboardu
 */
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
            // zformatovani GPS souradnic na 4 desetinna mista s Locale.US
            Text(
                text = "GPS: ${String.format(Locale.US, "%.4f", loc.latitude)}, ${String.format(Locale.US, "%.4f", loc.longitude)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * pomocna funkce pro ziskani jednorazove aktualni GPS polohy pres fusedloationclient
 */
@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
        .addOnSuccessListener { location -> onResult(location) }
        .addOnFailureListener { onResult(null) }
}

/**
 * Preklad GPS souradnic na textovou adresu pres geocoder
 * bezi na pozadi aby nezablokovala prekreslovani ui
 */
private fun fetchDetailedAddress(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
    // vytvoreni nove courutiny v io vlakne
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val addr = addresses?.firstOrNull()

            val resultText = if (addr != null) {
                val fullAddress = addr.getAddressLine(0)
                // ozerezani psc a statu z adresy
                fullAddress
                    .replace(", Česko", "")
                    .replace(", Czechia", "")
                    .replace(Regex("\\d{3}\\s?\\d{2}\\s"), "")
            } else {
                "Neznámá lokalita"
            }

            //funkce onresult ktera meni stavy v compose se vola zpatky na hlavnim ui vlakne aby app nespadla
            withContext(Dispatchers.Main) {
                onResult(resultText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult("Chyba zjišťování")
            }
        }
    }
}

/**
 * Komponenta zobrazujici kartu s mapou a informacemi o aktualnim pocasi
 */
@Composable
fun WeatherCard(
    hasLocationPermission: Boolean,
    address: String,
    weather: String,
    cameraPositionState: CameraPositionState,
    onPermissionRequest: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        // zbatveni vlastnimi odstiny barev pres theme.kt
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

            // box jako kontejner pro google mapu
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    // ismylocationenabled pro modou tecku na mape s polohou
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    // skryti tlacitek pro zoomovani mapy
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null)
                Spacer(Modifier.width(8.dp))
                Text(text = weather, style = MaterialTheme.typography.bodyLarge)
            }

            // pokud aplikace nema pravo prida na spodek karty tlacitko pro jeho vyzadani
            if (!hasLocationPermission) {
                Button(onClick = onPermissionRequest, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Povolit GPS")
                }
            }
        }
    }
}