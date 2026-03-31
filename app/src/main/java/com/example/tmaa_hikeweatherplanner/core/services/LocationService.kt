package com.example.tmaa_hikeweatherplanner.core.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Geocoder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tmaa_hikeweatherplanner.BuildConfig
import com.example.tmaa_hikeweatherplanner.MainActivity
import com.example.tmaa_hikeweatherplanner.core.network.WeatherApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * foreground sluzba ktere sleduje polohu a pocasi
 * kvuli tomu a zobrazovani trvale notifikace ji android neuspi ani pri minimializaci
 */
class LocationService : Service() {

    // Klient od googlu pro ziskavani gps souradnic
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // zpetne volani ktere se spusti pokazde kdyz se zmeni poloha
    private lateinit var locationCallback: LocationCallback

    // unikatni identifikator pro kanal notifikaci
    private val channelId = "location_tracking_channel"

    // coroutineScope pro sitove operace
    // dispatchers.io aby stahovani z internetu neblokovalo hlavni vlakno
    // supervisorjob zajistuje ze kdyby selhalo jedno stazeni nespadne cela sluzba
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // timestamp posledniho uspesneho stazeni dat z webu
    private var lastUpdateTimestamp: Long = 0

    // omezeni aby se api volalo maximalne jednou za minutu a nepoziralo data a baterku
    private val updateIntervalMs = 60000L

    // inicializace retrofit pro volani openweather api, vytvari se az pri prvnim pouziti
    private val apiService by lazy { WeatherApiService.create() }

    // metoda volana pouze jednou pri prvnim vytvoreni tehle sluzby v pameti
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    // metoda volana pokazde kdyz nekdo treba tlacitko v ui zavola startservice()
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // nejdrive se vytvori a zobrazi uvodni notifikace
        val notification = createNotification("Sledování aktivní", "Zjišťuji první data...")

        // donuceni systemu aby sluzbu drzel nazivu a ukazal notifikaci
        startForeground(1, notification)

        // zahajeni prubezneho sledovani gps
        startLocationUpdates()

        // START_STICKY rika systemu ze pokud ji zabije tak se ma snazit co nejdriv znovu spustit
        // kdyz to bude mozny
        return START_STICKY
    }

    // konfig a spusteni odeberu GPS souradnic
    private fun startLocationUpdates() {
        // co nejpresnejsi poloha s intervalem kontroly co 10 sekund
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        // definice co se stane pokud prijde nova poloha
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val currentTime = System.currentTimeMillis()

                // data se tahaji jednou za minutu aby se nebarly zdroje
                if (currentTime - lastUpdateTimestamp >= updateIntervalMs) {
                    result.lastLocation?.let { location ->
                        lastUpdateTimestamp = currentTime

                        // spusteni operace na pozadi mimo hlavni vlakno
                        serviceScope.launch {
                            val address = fetchAddressFromLocation(location.latitude, location.longitude)
                            val weather = fetchWeather(location.latitude, location.longitude)

                            // uprava vizualu notifikace se necha na hlavni vlakno
                            withContext(Dispatchers.Main) {
                                updateNotification(address, weather)
                            }
                        }
                    }
                }
            }
        }

        // odeslani zadosti systemu
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // volani  openweather api pro zjisteni pocasi na souradnicich
    private suspend fun fetchWeather(lat: Double, lon: Double): String {
        return try {
            val response = apiService.getCurrentWeather(lat, lon, BuildConfig.WEATHER_API_KEY)
            // naformatovani odpovedi do textu treba 20C° Jasno
            "${response.main.temp.toInt()}°C, ${response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""}"
        } catch (e: Exception) {
            e.printStackTrace()
            "Počasí nedostupné"
        }
    }

    // prelozeni gps souradnic na citelny nazev mista
    @Suppress("DEPRECATION")
    private fun fetchAddressFromLocation(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            // ziskani cele adresi pomoci regexu a odriznuti psc a statu
            addr?.getAddressLine(0)?.replace(", Česko", "")?.replace(Regex("\\d{3}\\s?\\d{2}\\s"), "") ?: "Neznámá lokalita"
        } catch (e: Exception) {
            e.printStackTrace()
            "Hledám adresu..."
        }
    }

    // sestaveni a nakonfigurovani vizualu notifikace
    private fun createNotification(address: String, weather: String): Notification {
        // PendingIntent definuje ze se po kliknuti na notifikaci otevre aplikace
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(address) // nadpis - nazev lokality
            .setContentText("Aktuálně: $weather") // text notifikace
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // zamezeni smazani notifikace
            .setSilent(true) // aby pri kazde aktualizaci necinkala nebo nevibrovala
            .setStyle(NotificationCompat.BigTextStyle().bigText("Aktuálně: $weather"))
            .build()
    }

    // prepsani notifikace novymi daty aby se nevytvarely porad nove
    private fun updateNotification(address: String, weather: String) {
        val notification = createNotification(address, weather)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // id 1 aby se porad premazavala ta puvodni notifikace
        notificationManager.notify(1, notification)
    }

    // vytvoreni kanalu pro notifikace
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId,
            "Sledování výletu",
            NotificationManager.IMPORTANCE_LOW // low aby se zprava ukazala bez zvuku
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    // metoda ktera se vola pri zruseni sluzby pokud ji uzivatel zastavi tlacitkem na profilu
    override fun onDestroy() {
        // zruseni vsech nedokoncenych stahovani dat z webu
        serviceScope.cancel()
        // odhlaseni od odberu polohy z telefonu
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    // metoda pro vazane sluzby ze service, nevyuzivam
    override fun onBind(intent: Intent?): IBinder? = null
}