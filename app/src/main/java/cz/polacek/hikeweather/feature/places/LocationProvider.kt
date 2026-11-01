package cz.polacek.hikeweather.feature.places

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/** Zjisteni aktualni polohy telefonu */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** vrati souradnice (lat, lon) nebo null, pokud poloha neni povolena nebo dostupna */
    @SuppressLint("MissingPermission") // opravneni se kontroluje v hasPermission()
    suspend fun currentLocation(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        return runCatching {
            val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
                ?: client.lastLocation.await()
            location?.let { it.latitude to it.longitude }
        }.getOrNull()
    }
}
