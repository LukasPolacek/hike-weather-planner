package cz.polacek.hikeweather.ui.trip

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.polacek.hikeweather.HikeWeatherApp
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.weather.DayForecast
import cz.polacek.hikeweather.feature.weather.ForecastSummarizer
import cz.polacek.hikeweather.feature.weather.capitalizeFirst
import cz.polacek.hikeweather.feature.weather.toRoundedInt
import cz.polacek.hikeweather.ui.components.AlertLine
import cz.polacek.hikeweather.ui.components.Formatters
import cz.polacek.hikeweather.ui.components.MapTarget
import cz.polacek.hikeweather.ui.components.MapyMap
import cz.polacek.hikeweather.ui.components.WeatherIcon
import cz.polacek.hikeweather.ui.search.TripDatePicker

/** Detail vyletu: mapa, pocasi v den vyletu, 5denni predpoved, uprava udaju a poznamka */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(factory = TripDetailViewModel.Factory)
) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }, enabled = trip != null) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val current = trip
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        TripContent(
            trip = current,
            weather = weather,
            contentPadding = padding,
            onPickDate = { showDatePicker = true },
            onDifficulty = viewModel::setDifficulty,
            onNote = viewModel::setNote
        )

        if (showDatePicker) {
            TripDatePicker(
                initial = current.plannedDate,
                onDismiss = { showDatePicker = false },
                onConfirm = {
                    viewModel.setDate(it)
                    showDatePicker = false
                }
            )
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.delete_trip_title)) },
                text = { Text(stringResource(R.string.delete_trip_text, current.name)) },
                confirmButton = {
                    TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripContent(
    trip: Trip,
    weather: TripWeatherState,
    contentPadding: PaddingValues,
    onPickDate: () -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    onNote: (String) -> Unit
) {
    val context = LocalContext.current
    val apiKey = (context.applicationContext as HikeWeatherApp).container.mapyApiKey
    // poznamka se edituje lokalne, do databaze se uklada s mirnym zpozdenim
    var note by remember(trip.id) { mutableStateOf(trip.note) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MapyMap(
            apiKey = apiKey,
            target = MapTarget(trip.latitude, trip.longitude, 13.0),
            interactive = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.large)
        )

        Column {
            Text(trip.name, style = MaterialTheme.typography.headlineSmall)
            if (trip.detail.isNotBlank()) {
                Text(trip.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trip.alertTitle?.let { AlertLine(it, trip.alertDetail) }

        // datum a narocnost
        AssistChip(
            onClick = onPickDate,
            label = { Text(trip.plannedDate?.let { "${Formatters.day(it)} · ${Formatters.relativeDay(it)}" } ?: stringResource(R.string.trip_date)) },
            leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) }
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Difficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    selected = trip.difficulty == difficulty,
                    onClick = { onDifficulty(difficulty) },
                    shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size)
                ) { Text(difficulty.label) }
            }
        }

        WeatherCard(trip, weather)

        if (weather.forecast.isNotEmpty()) {
            Text(stringResource(R.string.forecast_5_days), style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weather.forecast) { day -> ForecastDayChip(day, highlighted = day.date == trip.plannedDate) }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it; onNote(it) },
            label = { Text(stringResource(R.string.note)) },
            placeholder = { Text(stringResource(R.string.note_hint)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        FilledTonalButton(
            onClick = {
                // odkaz otevre aplikaci Mapy.com (pokud je nainstalovana) nebo web
                val url = "https://mapy.com/fnc/v1/showmap?mapset=outdoor&center=${trip.longitude},${trip.latitude}&zoom=15&marker=true"
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Map, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.navigate_mapy))
        }

        trip.forecastCheckedAt?.let {
            Text(
                stringResource(R.string.last_check, Formatters.dateTime(it)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WeatherCard(trip: Trip, weather: TripWeatherState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.weather_on_trip_day), style = MaterialTheme.typography.titleMedium)
            val date = trip.plannedDate
            val dayForecast = date?.let { ForecastSummarizer.forDate(weather.forecast, it) }
            when {
                weather.isLoading -> Text(stringResource(R.string.loading_weather))
                dayForecast != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(dayForecast.condition, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(dayForecast.summary, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Srážky ${Math.round(dayForecast.maxPrecipitationProbability * 100)} % · vítr ${dayForecast.maxWindSpeed.toRoundedInt()} m/s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                date != null -> Text(stringResource(R.string.forecast_not_yet), style = MaterialTheme.typography.bodyMedium)
                else -> Text(stringResource(R.string.trip_no_date), style = MaterialTheme.typography.bodyMedium)
            }
            weather.current?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(it.condition, size = 20)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.now_weather, it.summary), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (weather.error) Text(stringResource(R.string.no_internet), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ForecastDayChip(day: DayForecast, highlighted: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(Formatters.shortDay(day.date).capitalizeFirst(), style = MaterialTheme.typography.labelMedium)
            WeatherIcon(day.condition)
            Text("${day.maxTemperature.toRoundedInt()}°", style = MaterialTheme.typography.titleMedium)
            Text(
                "${day.minTemperature.toRoundedInt()}°",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
