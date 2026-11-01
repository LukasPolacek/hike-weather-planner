package cz.polacek.hikeweather.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.polacek.hikeweather.feature.alerts.WeatherCheckWorker
import cz.polacek.hikeweather.feature.trips.data.Difficulty
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import cz.polacek.hikeweather.feature.weather.CurrentWeather
import cz.polacek.hikeweather.feature.weather.DayForecast
import cz.polacek.hikeweather.feature.weather.ForecastSummarizer
import cz.polacek.hikeweather.feature.weather.WeatherAlerts
import cz.polacek.hikeweather.feature.weather.WeatherRepository
import cz.polacek.hikeweather.ui.appViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TripWeatherState(
    val isLoading: Boolean = true,
    val current: CurrentWeather? = null,
    val forecast: List<DayForecast> = emptyList(),
    val error: Boolean = false
)

class TripDetailViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository,
    private val weatherRepository: WeatherRepository,
    private val onDateChanged: () -> Unit = {}
) : ViewModel() {

    val trip: StateFlow<Trip?> = tripRepository.observeTrip(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _weather = MutableStateFlow(TripWeatherState())
    val weather: StateFlow<TripWeatherState> = _weather.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    private var noteJob: Job? = null

    init {
        viewModelScope.launch {
            val loaded = tripRepository.observeTrip(tripId).filterNotNull().first()
            loadWeather(loaded)
        }
    }

    fun refreshWeather() {
        trip.value?.let { viewModelScope.launch { loadWeather(it) } }
    }

    private suspend fun loadWeather(trip: Trip) {
        _weather.update { it.copy(isLoading = true, error = false) }
        val current = weatherRepository.current(trip.latitude, trip.longitude)
        val forecast = weatherRepository.dailyForecast(trip.latitude, trip.longitude)
        _weather.value = TripWeatherState(
            isLoading = false,
            current = current.getOrNull(),
            forecast = forecast.getOrDefault(emptyList()),
            error = current.isFailure && forecast.isFailure
        )
        // aktualni predpoved na den vyletu se rovnou ulozi do seznamu
        trip.plannedDate?.let { updateForecastSummary(trip, it) }
    }

    fun setDifficulty(difficulty: Difficulty) = update { it.copy(difficulty = difficulty) }

    fun setDate(date: LocalDate?) {
        val trip = trip.value ?: return
        viewModelScope.launch {
            val updated = trip.copy(
                plannedDate = date,
                weatherSummary = null,
                forecastMaxTemperature = null,
                alertTitle = null,
                alertDetail = null,
                forecastCheckedAt = null
            )
            tripRepository.save(updated)
            if (date != null) {
                updateForecastSummary(updated, date)
                onDateChanged()
            }
        }
    }

    /** poznamka se uklada az po kratke pauze v psani */
    fun setNote(note: String) {
        noteJob?.cancel()
        noteJob = viewModelScope.launch {
            delay(600)
            update { it.copy(note = note) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            tripRepository.delete(tripId)
            _deleted.send(Unit)
        }
    }

    private suspend fun updateForecastSummary(trip: Trip, date: LocalDate) {
        val day = ForecastSummarizer.forDate(_weather.value.forecast, date) ?: return
        val alert = WeatherAlerts.evaluate(day, trip.forecastMaxTemperature)
        tripRepository.save(
            trip.copy(
                weatherSummary = day.summary,
                forecastMaxTemperature = day.maxTemperature,
                alertTitle = alert?.type?.title,
                alertDetail = alert?.detail,
                forecastCheckedAt = System.currentTimeMillis()
            )
        )
    }

    private fun update(change: (Trip) -> Trip) {
        val current = trip.value ?: return
        viewModelScope.launch { tripRepository.save(change(current)) }
    }

    companion object {
        const val ARG_TRIP_ID = "tripId"

        val Factory = appViewModelFactory { c, savedState ->
            TripDetailViewModel(
                tripId = checkNotNull(savedState.get<String>(ARG_TRIP_ID)),
                tripRepository = c.tripRepository,
                weatherRepository = c.weatherRepository,
                onDateChanged = { WeatherCheckWorker.runNow(c.appContext) }
            )
        }
    }
}
