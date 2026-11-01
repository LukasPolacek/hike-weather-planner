package cz.polacek.hikeweather.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.polacek.hikeweather.AppContainer
import cz.polacek.hikeweather.HikeWeatherApp

/**
 * Tovarna na ViewModely - preda jim zavislosti z AppContaineru
 * SavedStateHandle obsahuje mimo jine argumenty navigace (napr. id vyletu)
 */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: (container: AppContainer, savedState: SavedStateHandle) -> VM
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HikeWeatherApp
        create(app.container, createSavedStateHandle())
    }
}
