package cz.polacek.hikeweather.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.polacek.hikeweather.core.prefs.UserPreferences
import cz.polacek.hikeweather.feature.alerts.WeatherCheckWorker
import cz.polacek.hikeweather.feature.backup.Account
import cz.polacek.hikeweather.feature.backup.AuthRepository
import cz.polacek.hikeweather.feature.backup.BackupRepository
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import cz.polacek.hikeweather.ui.appViewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TripStats(val total: Int = 0, val upcoming: Int = 0)

sealed interface ProfileEvent {
    data class BackupDone(val count: Int) : ProfileEvent
    data class RestoreDone(val count: Int) : ProfileEvent
    data class Failed(val message: String) : ProfileEvent
    data object SignInFailed : ProfileEvent
    data object DataDeleted : ProfileEvent
}

class ProfileViewModel(
    private val preferences: UserPreferences,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val backupRepository: BackupRepository,
    private val appContext: Context
) : ViewModel() {

    val userName: StateFlow<String?> = preferences.userName
    val alertsEnabled: StateFlow<Boolean> = preferences.weatherAlertsEnabled
    val lastBackupAt: StateFlow<Long?> = preferences.lastBackupAt
    val account: StateFlow<Account?> = authRepository.account

    val stats: StateFlow<TripStats> = tripRepository.observeTrips()
        .map { trips ->
            val today = LocalDate.now()
            TripStats(trips.size, trips.count { it.plannedDate?.isBefore(today) == false })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripStats())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun rename(name: String) {
        if (name.isNotBlank()) preferences.setUserName(name.trim())
    }

    fun setAlertsEnabled(enabled: Boolean) {
        preferences.setWeatherAlertsEnabled(enabled)
        if (enabled) {
            WeatherCheckWorker.schedule(appContext)
            WeatherCheckWorker.runNow(appContext)
        } else {
            WeatherCheckWorker.cancel(appContext)
        }
    }

    /** prihlaseni potrebuje kontext aktivity kvuli systemovemu dialogu s vyberem uctu */
    fun signIn(activityContext: Context) = runBusy {
        authRepository.signIn(activityContext).onFailure { _events.send(ProfileEvent.SignInFailed) }
    }

    fun signOut() = runBusy {
        authRepository.signOut(appContext)
        preferences.setLastBackupAt(null)
    }

    fun backup() = runBusy {
        backupRepository.backup()
            .onSuccess { _events.send(ProfileEvent.BackupDone(it)) }
            .onFailure { _events.send(ProfileEvent.Failed(it.localizedMessage ?: it.toString())) }
    }

    fun restore() = runBusy {
        backupRepository.restore()
            .onSuccess { _events.send(ProfileEvent.RestoreDone(it)) }
            .onFailure { _events.send(ProfileEvent.Failed(it.localizedMessage ?: it.toString())) }
    }

    /** smaze vylety a nastaveni v telefonu, zaloha v cloudu zustane */
    fun deleteAllData() = runBusy {
        tripRepository.deleteAll()
        WeatherCheckWorker.cancel(appContext)
        authRepository.signOut(appContext)
        preferences.clear()
        _events.send(ProfileEvent.DataDeleted)
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }

    companion object {
        val Factory = appViewModelFactory { c, _ ->
            ProfileViewModel(c.preferences, c.tripRepository, c.authRepository, c.backupRepository, c.appContext)
        }
    }
}
