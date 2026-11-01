package cz.polacek.hikeweather.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.polacek.hikeweather.HikeWeatherApp
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.feature.places.Place
import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.ui.components.Formatters
import cz.polacek.hikeweather.ui.components.MapyMap
import cz.polacek.hikeweather.ui.components.WeatherIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Hledani mista: turisticka mapa pres celou obrazovku, vyhledavani s naseptavanim
 * a panel s pocasim, datem a narocnosti pro ulozeni vyletu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenTrip: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val apiKey = (LocalContext.current.applicationContext as HikeWeatherApp).container.mapyApiKey
    val focusManager = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.trip_saved)
    val showText = stringResource(R.string.show)
    val duplicateText = stringResource(R.string.trip_duplicate)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.Saved -> {
                    val result = snackbar.showSnackbar(savedText, actionLabel = showText)
                    if (result == SnackbarResult.ActionPerformed) onOpenTrip(event.tripId)
                }
                SearchEvent.Duplicate -> snackbar.showSnackbar(duplicateText)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        MapyMap(
            apiKey = apiKey,
            target = state.mapTarget,
            modifier = Modifier.fillMaxSize(),
            onCenterChanged = viewModel::onMapCenterChanged
        )

        // vyhledavani nahore
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            SearchField(
                query = state.query,
                isSearching = state.isSearching,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearQuery,
                onSearch = { state.suggestions.firstOrNull()?.let { viewModel.onSuggestionSelected(it); focusManager.clearFocus() } }
            )
            if (state.suggestions.isNotEmpty() || state.noResults) {
                Spacer(Modifier.height(8.dp))
                SuggestionList(state.suggestions, state.noResults) { place ->
                    viewModel.onSuggestionSelected(place)
                    focusManager.clearFocus()
                }
            }
        }

        // panel s vybranym mistem dole
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            SnackbarHost(snackbar)
            if (state.selected != null) {
                PlacePanel(
                    state = state,
                    onPickDate = { showDatePicker = true },
                    onDifficulty = viewModel::onDifficultySelected,
                    onSave = viewModel::save
                )
            } else if (state.suggestions.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        stringResource(R.string.search_move_map),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        TripDatePicker(
            initial = state.plannedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.onDateSelected(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit
) {
    Surface(shape = MaterialTheme.shapes.large, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                when {
                    isSearching -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    query.isNotEmpty() -> IconButton(onClick = onClear) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SuggestionList(places: List<Place>, noResults: Boolean, onSelect: (Place) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (noResults) {
            Text(stringResource(R.string.search_no_results), Modifier.padding(16.dp))
            return@Card
        }
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(places) { place ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(place) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(place.name, style = MaterialTheme.typography.titleSmall)
                        if (place.detail.isNotBlank()) {
                            Text(
                                place.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
            // podminka Mapy.com - logo u vysledku vyhledavani
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Search by ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Image(painterResource(R.drawable.mapy_logo), "Mapy.com", Modifier.height(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlacePanel(
    state: SearchState,
    onPickDate: () -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    onSave: () -> Unit
) {
    val place = state.selected ?: return
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (place.detail.isNotBlank()) {
                        Text(
                            place.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (state.isLoadingPlace) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            // pocasi ted a pripadne predpoved na vybrany den
            val weather = state.currentWeather
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    weather != null -> {
                        WeatherIcon(weather.condition)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.now_weather, weather.summary), style = MaterialTheme.typography.bodyMedium)
                    }
                    state.weatherError -> Text(stringResource(R.string.weather_unavailable), style = MaterialTheme.typography.bodyMedium)
                    else -> Text(stringResource(R.string.loading_weather), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.plannedDate != null) {
                val forecast = state.forecastForDate
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (forecast != null) {
                        WeatherIcon(forecast.condition)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.forecast_for_day, forecast.summary), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            stringResource(R.string.forecast_not_yet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AssistChip(
                onClick = onPickDate,
                label = {
                    Text(state.plannedDate?.let { Formatters.day(it) } ?: stringResource(R.string.trip_date))
                },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) }
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Difficulty.entries.forEachIndexed { index, difficulty ->
                    SegmentedButton(
                        selected = state.difficulty == difficulty,
                        onClick = { onDifficulty(difficulty) },
                        shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size)
                    ) { Text(difficulty.label) }
                }
            }

            Button(onClick = onSave, enabled = !state.isSaving && !state.isLoadingPlace, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_trip))
            }
        }
    }
}

/** Vyber data vyletu - jen dnesek a budouci dny */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDatePicker(initial: LocalDate?, onDismiss: () -> Unit, onConfirm: (LocalDate?) -> Unit) {
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val date = pickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                onConfirm(date)
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) { Text(stringResource(R.string.trip_no_date)) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}
