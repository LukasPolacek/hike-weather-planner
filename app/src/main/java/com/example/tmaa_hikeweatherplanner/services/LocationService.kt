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

// Service bezici na pozadi ktery sleduje polohu a pocasi
class LocationService : Service() {

    // Google klient pro ziskani polohy
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Zpetne volani pri zjisteni nove polhoy
    private lateinit var locationCallback: LocationCallback

    // Identifikator notifikaci
    private val channelId = "location_tracking_channel"

    // Vlastni vlakno CoroutineScope pro provadeni sitovych operaci mimo hlavni UI vlakno
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cas posledniho stahnuti dat z webu
    private var lastUpdateTimestamp: Long = 0

    // Stahovani dat maximalne jednou za 60s
    private val UPDATE_INTERVAL_MS = 60000L

    // Volani API s pocasim
    private val apiService by lazy { WeatherApiService.create() }

    // Funkce volana pri prvnim vytvoreni teto sluzby
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    // Funkce volana pokazde kdyz nekdo  zavola startService()
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Uvodni notifikace pred ziskanim prvnich dat
        val notification = createNotification("Sledování aktivní", "Zjišťuji první data...")

        // Prikazani ze sluzba bezi v popredi a nesmi byt uspana
        startForeground(1, notification)

        // Prubezne sledovani GPS
        startLocationUpdates()

        // Pokus o znovuzapnti pokud system vypne sluzbu
        return START_STICKY
    }

    // Nastaveni a spusteni odberu zmen polohy
    private fun startLocationUpdates() {
        // Konfigurace pozadavku na polohu kazdych 10s
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        // Pri ziskani nove polohy
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val currentTime = System.currentTimeMillis()

                // Kontrola zda ubehlo 60s
                if (currentTime - lastUpdateTimestamp >= UPDATE_INTERVAL_MS) {
                    result.lastLocation?.let { location ->
                        lastUpdateTimestamp = currentTime

                        // Spusteni stahovani adresy a pocasi na pozadi
                        serviceScope.launch {
                            val address = fetchAddressFromLocation(location.latitude, location.longitude)
                            val weather = fetchWeather(location.latitude, location.longitude)

                            // Aktualizace notifikace na hlavnim vlakne
                            withContext(Dispatchers.Main) {
                                updateNotification(address, weather)
                            }
                        }
                    }
                }
            }
        }

        // Zadost o odber polohy (ignorace pri chybejicim opravneni)
        try { fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null) } catch (e: SecurityException) { }
    }

    // Stazeni dat z OpenWeather API podle aktualnich souradnic
    private suspend fun fetchWeather(lat: Double, lon: Double): String {
        return try {
            val response = apiService.getCurrentWeather(lat, lon, BuildConfig.WEATHER_API_KEY)
            "${response.main.temp.toInt()}°C, ${response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""}"
        } catch (e: Exception) { "Počasí nedostupné" }
    }

    // Prelozeni souradnic pomoci Geocoderu na textovou adresu
    private fun fetchAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            addr?.getAddressLine(0)?.replace(", Česko", "")?.replace(Regex("\\d{3}\\s?\\d{2}\\s"), "") ?: "Neznámá lokalita"
        } catch (e: Exception) { "Hledám adresu..." }
    }

    // Sestaveni vizualu notifikace
    private fun createNotification(address: String, weather: String): Notification {
        // Intent ktery po kliknuti na notifikaci otevre MainActivity
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(address)
            .setContentText("Aktuálně: $weather")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Notifikaci nelze smazat odsunutim
            .setSilent(true) // Tichy rezim, aby necinkala pri kazde aktualizaci
            .setStyle(NotificationCompat.BigTextStyle().bigText("Aktuálně: $weather"))
            .build()
    }

    // Prepis stavajici notifikaci novymi daty
    private fun updateNotification(address: String, weather: String) {
        val notification = createNotification(address, weather)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Id 1 pro prepsani notifikace a nevytvareni nove
        notificationManager.notify(1, notification)
    }

    // Vytvoreni kanalu pro notifikace
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(channelId, "Sledování výletu", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // Volano pri zruseni sluzby (stop tlacitko)
    override fun onDestroy() {
        // Ukonceni stahovani na pozadi
        serviceScope.cancel()
        // Prestani odebirani polohy z GPS senzoru
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    // Metoda pro vazane sluzby, nevuzivam
    override fun onBind(intent: Intent?): IBinder? = null
}