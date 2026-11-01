package cz.polacek.hikeweather

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import cz.polacek.hikeweather.feature.alerts.Notifications
import cz.polacek.hikeweather.ui.HikeWeatherRoot
import cz.polacek.hikeweather.ui.theme.HikeWeatherTheme

/** Jedina aktivita aplikace, obsah je cely v Jetpack Compose */
class MainActivity : ComponentActivity() {

    // vylet, ktery se ma otevrit po kliknuti na upozorneni na pocasi
    private val pendingTripId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingTripId.value = intent.getStringExtra(Notifications.EXTRA_TRIP_ID)

        val container = (application as HikeWeatherApp).container
        setContent {
            HikeWeatherTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HikeWeatherRoot(
                        container = container,
                        pendingTripId = pendingTripId.value,
                        onTripOpened = { pendingTripId.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifications.EXTRA_TRIP_ID)?.let { pendingTripId.value = it }
    }
}
