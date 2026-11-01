package cz.polacek.hikeweather.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.polacek.hikeweather.feature.alerts.WeatherCheckWorker
import cz.polacek.hikeweather.feature.places.LocationProvider
import cz.polacek.hikeweather.feature.places.Place
import cz.polacek.hikeweather.feature.places.PlacesRepository
import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import cz.polacek.hikeweather.feature.trips.data.containsSamePlace
import cz.polacek.hikeweather.feature.weather.CurrentWeather
import cz.polacek.hikeweather.feature.weather.DayForecast
import cz.polacek.hikeweather.feature.weather.ForecastSummarizer
import cz.polacek.hikeweather.feature.weather.WeatherRepository
import cz.polacek.hikeweather.ui.appViewModelFactory
import cz.polacek.hikeweather.ui.components.MapTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

data class SearchState(
    val query: String = "",
    val suggestions: List<Place> = emptyList(),
    val isSearching: Boolean = false,
    val noResults: Boolean = false,
    val mapTarget: MapTarget = MapTarget(49.8, 15.5, 6.5), // stred Ceska
    val selected: Place? = null,
    val isLoadingPlace: Boolean = false,
    val currentWeather: CurrentWeather? = null,
    val forecast: List<DayForecast> = emptyList(),
    val weatherError: Boolean = false,
    val plannedDate: LocalDate? = null,
    val difficulty: Difficulty = Difficulty.EASY,
    val isSaving: Boolean = false
) {
    /** predpoved pro vybrany den, pokud uz je v dosahu predpovedi */
    val forecastForDate: DayForecast?
        get() = plannedDate?.let { ForecastSummarizer.forDate(forecast, it) }
}

sealed interface SearchEvent {
    data class Saved(val tripId: String) : SearchEvent
    data object Duplicate : SearchEvent
}

class SearchViewModel(
    private val placesRepository: PlacesRepository,
    private val weatherRepository: WeatherRepository,
    private val tripRepository: TripRepository,
    private val locationProvider: LocationProvider?,
    private val onTripSaved: () -> Unit = {}
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var suggestJob: Job? = null
    private var placeJob: Job? = null

    init {
        // mapa zacne na poloze uzivatele, pokud je k dispozici
        viewModelScope.launch {
            locationProvider?.currentLocation()?.let { (lat, lon) ->
                if (_state.value.selected == null) _state.update { it.copy(mapTarget = MapTarget(lat, lon, 12.0)) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, noResults = false) }
        suggestJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(suggestions = emptyList(), isSearching = false) }
            return
        }
        // naseptavani az po kratke pauze v psani, at se API nevola pri kazdem pismenu
        suggestJob = viewModelScope.launch {
            delay(SUGGEST_DELAY_MS)
            _state.update { it.copy(isSearching = true) }
            val result = placesRepository.suggest(query).getOrDefault(emptyList())
            _state.update { it.copy(suggestions = result, isSearching = false, noResults = result.isEmpty()) }
        }
    }

    fun clearQuery() {
        suggestJob?.cancel()
        _state.update { it.copy(query = "", suggestions = emptyList(), noResults = false, isSearching = false) }
    }

    fun onSuggestionSelected(place: Place) {
        suggestJob?.cancel()
        placeJob?.cancel()
        _state.update {
            it.copy(
                query = place.name,
                suggestions = emptyList(),
                noResults = false,
                selected = place,
                mapTarget = MapTarget(place.latitude, place.longitude, 14.0)
            )
        }
        loadWeather(place)
    }

    /** po posunuti mapy se misto pod znackou nacte znovu */
    fun onMapCenterChanged(lat: Double, lon: Double) {
        val selected = _state.value.selected
        // posun mapy na prave vybrane misto (z vyhledavani) neni novy vyber
        if (selected != null && isSamePlace(selected.latitude, selected.longitude, lat, lon)) return
        val target = _state.value.mapTarget
        // uvodni zobrazeni mapy bez posunu uzivatelem jeste nic nevybira
        if (selected == null && isSamePlace(target.latitude, target.longitude, lat, lon)) return

        placeJob?.cancel()
        placeJob = viewModelScope.launch {
            delay(PLACE_DELAY_MS)
            _state.update { it.copy(isLoadingPlace = true) }
            val place = placesRepository.placeAt(lat, lon).getOrNull()
                ?: Place(cz.polacek.hikeweather.feature.places.formatCoordinates(lat, lon), "", lat, lon)
            _state.update { it.copy(selected = place, query = place.name, isLoadingPlace = false) }
            loadWeather(place)
        }
    }

    fun onDateSelected(date: LocalDate?) = _state.update { it.copy(plannedDate = date) }

    fun onDifficultySelected(difficulty: Difficulty) = _state.update { it.copy(difficulty = difficulty) }

    fun save() {
        val current = _state.value
        val place = current.selected ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val forecast = current.forecastForDate
            val trip = Trip(
                name = place.name,
                detail = place.detail,
                latitude = place.latitude,
                longitude = place.longitude,
                difficulty = current.difficulty,
                plannedDate = current.plannedDate,
                weatherSummary = forecast?.summary ?: current.currentWeather?.summary,
                forecastMaxTemperature = forecast?.maxTemperature,
                forecastCheckedAt = forecast?.let { System.currentTimeMillis() }
            )
            if (tripRepository.getTrips().containsSamePlace(trip)) {
                _events.send(SearchEvent.Duplicate)
            } else {
                tripRepository.save(trip)
                if (trip.plannedDate != null) onTripSaved()
                _events.send(SearchEvent.Saved(trip.id))
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun loadWeather(place: Place) {
        viewModelScope.launch {
            _state.update { it.copy(currentWeather = null, forecast = emptyList(), weatherError = false) }
            val current = weatherRepository.current(place.latitude, place.longitude)
            val forecast = weatherRepository.dailyForecast(place.latitude, place.longitude)
            // vysledek pouzit jen pokud mezitim uzivatel nevybral jine misto
            if (_state.value.selected == place) {
                _state.update {
                    it.copy(
                        currentWeather = current.getOrNull(),
                        forecast = forecast.getOrDefault(emptyList()),
                        weatherError = current.isFailure
                    )
                }
            }
        }
    }

    companion object {
        private const val SUGGEST_DELAY_MS = 350L
        private const val PLACE_DELAY_MS = 400L

        /** do cca 50 m se bere jako stejne misto */
        fun isSamePlace(lat1: Double, lon1: Double, lat2: Double, lon2: Double) =
            abs(lat1 - lat2) < 0.0005 && abs(lon1 - lon2) < 0.0005

        val Factory = appViewModelFactory { c, _ ->
            SearchViewModel(
                c.placesRepository, c.weatherRepository, c.tripRepository, c.locationProvider,
                onTripSaved = { WeatherCheckWorker.runNow(c.appContext) }
            )
        }
    }
}
