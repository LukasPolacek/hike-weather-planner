package com.example.tmaa_hikeweatherplanner.feature.hike.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tmaa_hikeweatherplanner.MyApplication
import com.example.tmaa_hikeweatherplanner.feature.hike.data.repository.LocationRepository
import com.example.tmaa_hikeweatherplanner.feature.hike.data.local.SavedLocation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Hlavni viewmodel pro spravovani vyletu
 * Kvuli oddeleni aplikacni a databazove logiky od ui
 * Drzi stav dat i pri otoceni telefonu
 */
class HikeViewModel(
    private val repository: LocationRepository
) : ViewModel() {

    // Ziskani dat z databaze pres flow a jejich prevod na stateflow
    // a ze stateflow jetpack compose pak cte samotna data a prekresli ui
    // stateIn aby se data z repozitare nacitala na pozadi
    // sharingstarted.lazily aby se data zacla nacitat az v okamziku kdy je ui poprve pozaduje
    val savedLocations: StateFlow<List<SavedLocation>> = repository.getAllLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Ulozeni nove lokality do databaze.
    fun saveLocation(city: String, difficulty: String, weather: String, lat: Double, lon: Double) {
        // vsechna prace s databazi uvnitr viewemodelscope
        // asynchrone aby neblokovala ui vlakno
        // pri zniceni viewmodelu se bezpecne ukonci
        viewModelScope.launch {
            repository.addLocation(
                SavedLocation(
                    city = city,
                    difficulty = difficulty,
                    weather = weather,
                    latitude = lat,
                    longitude = lon
                )
            )
        }
    }

    // Smazani konkretniho vyletu z historie
    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    // Vymazani vsech ulozenych vyletu pri resetu profilu
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLocations()
        }
    }

    /**
     * Factory pro vytvoreni instance hikeviewmodelu
     * rika androidu kde ze vzit repozitar z myapplication pri vytvoreni viewmodelu
     */
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)
                HikeViewModel(repository = application.locationRepository)
            }
        }
    }
}