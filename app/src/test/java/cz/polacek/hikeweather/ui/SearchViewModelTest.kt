package cz.polacek.hikeweather.ui

import cz.polacek.hikeweather.feature.places.Place
import cz.polacek.hikeweather.feature.places.PlacesRepository
import cz.polacek.hikeweather.feature.trips.data.Trip
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import cz.polacek.hikeweather.feature.weather.CurrentWeather
import cz.polacek.hikeweather.feature.weather.DayForecast
import cz.polacek.hikeweather.feature.weather.WeatherCondition
import cz.polacek.hikeweather.feature.weather.WeatherRepository
import cz.polacek.hikeweather.ui.search.SearchEvent
import cz.polacek.hikeweather.ui.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val praded = Place("Praděd", "Vrchol", 50.0832, 17.2309)
    private val tomorrow = LocalDate.now().plusDays(1)

    private val places = object : PlacesRepository {
        override suspend fun suggest(query: String) = Result.success(listOf(praded))
        override suspend fun placeAt(lat: Double, lon: Double) = Result.success(Place("Bod", "", lat, lon))
    }

    private val weather = object : WeatherRepository {
        override suspend fun current(lat: Double, lon: Double) = Result.success(
            CurrentWeather(2.0, -3.0, "mlha", WeatherCondition.FOG, 9.0, 95, null, null)
        )
        override suspend fun dailyForecast(lat: Double, lon: Double) = Result.success(
            listOf(DayForecast(tomorrow, -1.0, 4.0, WeatherCondition.SNOW, "sněžení", 14.0, 0.8))
        )
    }

    private class FakeTrips : TripRepository {
        val trips = MutableStateFlow<List<Trip>>(emptyList())
        override fun observeTrips() = trips
        override fun observeTrip(id: String) = trips.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun getTrips() = trips.value
        override suspend fun save(trip: Trip) { trips.value = trips.value.filterNot { it.id == trip.id } + trip }
        override suspend fun saveAll(trips: List<Trip>) = trips.forEach { save(it) }
        override suspend fun delete(id: String) { trips.value = trips.value.filterNot { it.id == id } }
        override suspend fun deleteAll() { trips.value = emptyList() }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `vyber z naseptavace nacte pocasi a ulozi vylet s predpovedi na zvoleny den`() = runTest(dispatcher) {
        val trips = FakeTrips()
        var scheduled = 0
        val viewModel = SearchViewModel(places, weather, trips, locationProvider = null, onTripSaved = { scheduled++ })

        viewModel.onQueryChange("Prad")
        advanceUntilIdle()
        assertEquals(listOf(praded), viewModel.state.value.suggestions)

        viewModel.onSuggestionSelected(praded)
        viewModel.onDateSelected(tomorrow)
        advanceUntilIdle()
        assertEquals("Praděd", viewModel.state.value.selected?.name)
        assertEquals(WeatherCondition.SNOW, viewModel.state.value.forecastForDate?.condition)

        viewModel.save()
        advanceUntilIdle()
        val saved = trips.trips.value.single()
        assertEquals(tomorrow, saved.plannedDate)
        assertEquals(4.0, saved.forecastMaxTemperature!!, 0.01)
        assertEquals(1, scheduled)
        assertTrue(viewModel.events.first() is SearchEvent.Saved)
    }

    @Test
    fun `stejny vylet podruhe se neulozi`() = runTest(dispatcher) {
        val trips = FakeTrips()
        val viewModel = SearchViewModel(places, weather, trips, locationProvider = null)
        viewModel.onSuggestionSelected(praded)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()
        assertTrue(viewModel.events.first() is SearchEvent.Saved)

        viewModel.save()
        advanceUntilIdle()
        assertEquals(SearchEvent.Duplicate, viewModel.events.first())
        assertEquals(1, trips.trips.value.size)
    }
}
