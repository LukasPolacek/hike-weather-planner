package cz.polacek.hikeweather.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.polacek.hikeweather.feature.places.LocationProvider
import cz.polacek.hikeweather.feature.places.PlacesRepository
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import cz.polacek.hikeweather.feature.weather.CurrentWeather
import cz.polacek.hikeweather.feature.weather.WeatherRepository
import cz.polacek.hikeweather.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Stav pocasi v miste uzivatele */
data class LocalWeatherState(
    val isLoading: Boolean = true,
    val hasLocationPermission: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
    val placeDetail: String? = null,
    val weather: CurrentWeather? = null,
    val error: Boolean = false
)

/** Vylety rozdelene pro prehled */
data class TripSections(
    val upcoming: List<Trip> = emptyList(),
    val undated: List<Trip> = emptyList(),
    val past: List<Trip> = emptyList()
) {
    val isEmpty: Boolean get() = upcoming.isEmpty() && undated.isEmpty() && past.isEmpty()
}

class DashboardViewModel(
    tripRepository: TripRepository,
    private val weatherRepository: WeatherRepository,
    private val placesRepository: PlacesRepository,
    private val locationProvider: LocationProvider,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _local = MutableStateFlow(LocalWeatherState())
    val local: StateFlow<LocalWeatherState> = _local.asStateFlow()

    val trips: StateFlow<TripSections> = tripRepository.observeTrips()
        .map { splitTrips(it, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripSections())

    init {
        refresh()
    }

    /** nacte polohu, nazev mista a pocasi (i pri stazeni seznamu dolu) */
    fun refresh() {
        viewModelScope.launch {
            if (!locationProvider.hasPermission()) {
                _local.value = LocalWeatherState(isLoading = false, hasLocationPermission = false)
                return@launch
            }
            _local.update { it.copy(isLoading = true, hasLocationPermission = true, error = false) }
            val location = locationProvider.currentLocation()
            if (location == null) {
                _local.update { it.copy(isLoading = false, error = true) }
                return@launch
            }
            val (lat, lon) = location
            val place = placesRepository.placeAt(lat, lon).getOrNull()
            val weather = weatherRepository.current(lat, lon)
            _local.value = LocalWeatherState(
                isLoading = false,
                latitude = lat,
                longitude = lon,
                placeName = place?.name,
                placeDetail = place?.detail?.takeIf { it.isNotBlank() },
                weather = weather.getOrNull(),
                error = weather.isFailure
            )
        }
    }

    companion object {
        val Factory = appViewModelFactory { c, _ ->
            DashboardViewModel(c.tripRepository, c.weatherRepository, c.placesRepository, c.locationProvider)
        }

        /** rozdeleni vyletu na nadchazejici (podle data), bez data a probehle (od nejnovejsiho) */
        fun splitTrips(trips: List<Trip>, today: LocalDate): TripSections {
            val (dated, undated) = trips.partition { it.plannedDate != null }
            val (past, upcoming) = dated.partition { it.plannedDate!!.isBefore(today) }
            return TripSections(
                upcoming = upcoming.sortedBy { it.plannedDate },
                undated = undated.sortedByDescending { it.createdAt },
                past = past.sortedByDescending { it.plannedDate }
            )
        }
    }
}
