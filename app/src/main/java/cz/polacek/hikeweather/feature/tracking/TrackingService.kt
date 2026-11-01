package cz.polacek.hikeweather.feature.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import cz.polacek.hikeweather.HikeWeatherApp
import cz.polacek.hikeweather.MainActivity
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.feature.alerts.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sledovani prave probihajiciho vyletu
 * sluzba na popredi s trvalou notifikaci, ve ktere prubezne ukazuje misto a pocasi
 * kvuli trvale notifikaci ji Android neukonci ani pri zhasnutem displeji
 */
class TrackingService : Service() {

    private lateinit var locationClient: FusedLocationProviderClient
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastUpdate = 0L

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val now = System.currentTimeMillis()
            // pocasi a adresa maximalne jednou za minutu, aby se setrila baterie a data
            if (now - lastUpdate < UPDATE_INTERVAL_MS) return
            lastUpdate = now

            scope.launch {
                val container = (application as HikeWeatherApp).container
                val place = container.placesRepository.placeAt(location.latitude, location.longitude).getOrNull()
                val weather = container.weatherRepository.current(location.latitude, location.longitude).getOrNull()
                val title = place?.name ?: getString(R.string.tracking_unknown_place)
                val text = weather?.summary ?: getString(R.string.weather_unavailable)
                getSystemService(android.app.NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(title, text))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission") // opravneni k poloze se overi v UI pred spustenim sluzby
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // tlacitko Ukoncit v notifikaci
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = buildNotification(getString(R.string.tracking_active), getString(R.string.tracking_waiting))
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        runCatching { locationClient.requestLocationUpdates(request, callback, mainLooper) }
        _isRunning.value = true
        return START_STICKY
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, TrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Notifications.CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.tracking_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        locationClient.removeLocationUpdates(callback)
        scope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL_MS = 60_000L
        private const val ACTION_STOP = "stop"

        private val _isRunning = MutableStateFlow(false)
        /** jestli sledovani prave bezi - pro prepinac v UI */
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, TrackingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackingService::class.java))
        }
    }
}
