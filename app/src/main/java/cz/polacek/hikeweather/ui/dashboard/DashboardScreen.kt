package cz.polacek.hikeweather.ui.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.feature.weather.capitalizeFirst
import cz.polacek.hikeweather.feature.weather.toRoundedInt
import cz.polacek.hikeweather.ui.components.EmptyState
import cz.polacek.hikeweather.ui.components.Formatters
import cz.polacek.hikeweather.ui.components.SectionTitle
import cz.polacek.hikeweather.ui.components.TripCard
import cz.polacek.hikeweather.ui.components.WeatherIcon
import java.time.LocalDate

/**
 * Prehled: pozdrav, pocasi v miste uzivatele a seznam vyletu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    onOpenTrip: (String) -> Unit,
    onSearch: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val local by viewModel.local.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refresh()
    }

    PullToRefreshBox(
        isRefreshing = local.isLoading && local.weather != null,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            item {
                Text(
                    Formatters.day(LocalDate.now()).capitalizeFirst(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(stringResource(R.string.greeting, userName), style = MaterialTheme.typography.headlineMedium)
            }

            item {
                LocalWeatherCard(
                    state = local,
                    onEnableLocation = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                    onRetry = viewModel::refresh
                )
            }

            if (trips.isEmpty) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Hiking,
                        title = stringResource(R.string.no_trips_title),
                        text = stringResource(R.string.no_trips_text)
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(onClick = onSearch) {
                            Icon(Icons.Filled.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.find_place))
                        }
                    }
                }
            }

            tripSection(R.string.upcoming_trips, trips.upcoming, onOpenTrip)
            tripSection(R.string.other_trips, trips.undated, onOpenTrip)
            tripSection(R.string.past_trips, trips.past, onOpenTrip)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tripSection(
    titleRes: Int,
    trips: List<cz.polacek.hikeweather.feature.trips.data.Trip>,
    onOpenTrip: (String) -> Unit
) {
    if (trips.isEmpty()) return
    item(key = "title_$titleRes") {
        SectionTitle(stringResource(titleRes), Modifier.padding(top = 12.dp))
    }
    items(trips, key = { it.id }) { trip ->
        TripCard(trip = trip, onClick = { onOpenTrip(trip.id) })
    }
}

/** Vyrazna karta s pocasim v miste, kde uzivatel prave je */
@Composable
private fun LocalWeatherCard(state: LocalWeatherState, onEnableLocation: () -> Unit, onRetry: () -> Unit) {
    val gradient = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
    )
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            val onCard = MaterialTheme.colorScheme.onPrimary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MyLocation, null, tint = onCard.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    state.placeName ?: stringResource(R.string.current_location),
                    style = MaterialTheme.typography.titleMedium,
                    color = onCard,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            state.placeDetail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = onCard.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))

            val weather = state.weather
            when {
                !state.hasLocationPermission -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOff, null, tint = onCard)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.location_unknown), color = onCard)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onEnableLocation) {
                        Text(stringResource(R.string.enable_location), color = onCard)
                    }
                }
                weather != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(onCard.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            WeatherIcon(weather.condition, size = 40)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "${weather.temperature.toRoundedInt()} °C",
                                style = MaterialTheme.typography.displaySmall,
                                color = onCard
                            )
                            Text(weather.description.capitalizeFirst(), style = MaterialTheme.typography.titleMedium, color = onCard)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        weather.feelsLike?.let {
                            WeatherFact(null, stringResource(R.string.feels_like, it.toRoundedInt()))
                        }
                        weather.windSpeed?.let {
                            WeatherFact(Icons.Filled.Air, stringResource(R.string.wind, it.toRoundedInt()))
                        }
                    }
                    if (weather.sunrise != null && weather.sunset != null) {
                        Spacer(Modifier.height(6.dp))
                        WeatherFact(
                            Icons.Filled.WbTwilight,
                            stringResource(R.string.sunrise_sunset, Formatters.time(weather.sunrise), Formatters.time(weather.sunset))
                        )
                    }
                }
                state.isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = onCard, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.loading_weather), color = onCard)
                    }
                }
                else -> {
                    Text(stringResource(R.string.no_internet), color = onCard)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = onCard) }
                }
            }
        }
    }
}

@Composable
private fun WeatherFact(icon: ImageVector?, text: String) {
    val color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
