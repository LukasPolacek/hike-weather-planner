package com.example.tmaa_hikeweatherplanner.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.example.tmaa_hikeweatherplanner.MainActivity
import com.example.tmaa_hikeweatherplanner.BuildConfig
import com.example.tmaa_hikeweatherplanner.network.WeatherApiService
import kotlinx.coroutines.*
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val channelId = "location_tracking_channel"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastUpdateTimestamp: Long = 0
    private val UPDATE_INTERVAL_MS = 60000L

    private val apiService by lazy { WeatherApiService.create() }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Sledování aktivní", "Zjišťuji první data...")
        startForeground(1, notification)
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTimestamp >= UPDATE_INTERVAL_MS) {
                    result.lastLocation?.let { location ->
                        lastUpdateTimestamp = currentTime
                        serviceScope.launch {
                            val address = fetchAddressFromLocation(location.latitude, location.longitude)
                            val weather = fetchWeather(location.latitude, location.longitude)
                            withContext(Dispatchers.Main) {
                                updateNotification(address, weather)
                            }
                        }
                    }
                }
            }
        }

        try { fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null) } catch (e: SecurityException) { }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double): String {
        return try {
            val response = apiService.getCurrentWeather(lat, lon, BuildConfig.WEATHER_API_KEY)
            "${response.main.temp.toInt()}°C, ${response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""}"
        } catch (e: Exception) { "Počasí nedostupné" }
    }

    private fun fetchAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            addr?.getAddressLine(0)?.replace(", Česko", "")?.replace(Regex("\\d{3}\\s?\\d{2}\\s"), "") ?: "Neznámá lokalita"
        } catch (e: Exception) { "Hledám adresu..." }
    }

    private fun createNotification(address: String, weather: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(address).setContentText("Aktuálně: $weather").setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent).setOngoing(true).setSilent(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Aktuálně: $weather")).build()
    }

    private fun updateNotification(address: String, weather: String) {
        val notification = createNotification(address, weather)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(channelId, "Sledování výletu", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}