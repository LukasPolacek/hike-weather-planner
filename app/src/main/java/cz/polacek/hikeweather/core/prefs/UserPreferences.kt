package cz.polacek.hikeweather.core.prefs

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Jednoducha nastaveni aplikace v SharedPreferences
 * hodnoty se zaroven drzi ve StateFlow, aby se UI samo prekreslilo pri zmene
 */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString(KEY_NAME, null))
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _weatherAlerts = MutableStateFlow(prefs.getBoolean(KEY_ALERTS, true))
    val weatherAlertsEnabled: StateFlow<Boolean> = _weatherAlerts.asStateFlow()

    private val _lastBackupAt = MutableStateFlow(prefs.getLong(KEY_BACKUP, 0L).takeIf { it > 0 })
    val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

    fun setUserName(name: String) {
        prefs.edit { putString(KEY_NAME, name) }
        _userName.value = name
    }

    fun setWeatherAlertsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ALERTS, enabled) }
        _weatherAlerts.value = enabled
    }

    fun setLastBackupAt(timestamp: Long?) {
        prefs.edit { if (timestamp == null) remove(KEY_BACKUP) else putLong(KEY_BACKUP, timestamp) }
        _lastBackupAt.value = timestamp
    }

    fun clear() {
        prefs.edit { clear() }
        _userName.value = null
        _weatherAlerts.value = true
        _lastBackupAt.value = null
    }

    private companion object {
        const val KEY_NAME = "user_name"
        const val KEY_ALERTS = "weather_alerts"
        const val KEY_BACKUP = "last_backup_at"
    }
}
