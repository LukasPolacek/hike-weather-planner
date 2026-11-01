package cz.polacek.hikeweather

import android.app.Application
import android.content.Context
import cz.polacek.hikeweather.core.database.AppDatabase
import cz.polacek.hikeweather.core.network.MapyApi
import cz.polacek.hikeweather.core.network.OpenWeatherApi
import cz.polacek.hikeweather.core.prefs.UserPreferences
import cz.polacek.hikeweather.feature.alerts.Notifications
import cz.polacek.hikeweather.feature.alerts.WeatherCheckWorker
import cz.polacek.hikeweather.feature.backup.AuthRepository
import cz.polacek.hikeweather.feature.backup.BackupRepository
import cz.polacek.hikeweather.feature.places.LocationProvider
import cz.polacek.hikeweather.feature.places.MapyPlacesRepository
import cz.polacek.hikeweather.feature.places.PlacesRepository
import cz.polacek.hikeweather.feature.trips.data.RoomTripRepository
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import cz.polacek.hikeweather.feature.weather.OpenWeatherRepository
import cz.polacek.hikeweather.feature.weather.WeatherRepository
import org.maplibre.android.MapLibre
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Trida aplikace - spousti se jako prvni a drzi sdilene zavislosti
 */
class HikeWeatherApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // MapLibre se musi inicializovat pred zobrazenim prvni mapy
        MapLibre.getInstance(this)
        Notifications.createChannels(this)
        if (container.preferences.weatherAlertsEnabled.value) {
            WeatherCheckWorker.schedule(this)
        }
    }
}

/**
 * Jednoduchy kontejner zavislosti (rucni dependency injection)
 * kazda trida dostane sve zavislosti v konstruktoru, takze jdou v testech nahradit
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val preferences = UserPreferences(appContext)

    private val database by lazy { AppDatabase.create(appContext) }

    val tripRepository: TripRepository by lazy { RoomTripRepository(database.tripDao()) }

    val weatherRepository: WeatherRepository by lazy {
        val api = Retrofit.Builder()
            .baseUrl(OpenWeatherApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherApi::class.java)
        OpenWeatherRepository(api, BuildConfig.WEATHER_API_KEY)
    }

    val placesRepository: PlacesRepository by lazy {
        val api = Retrofit.Builder()
            .baseUrl(MapyApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MapyApi::class.java)
        MapyPlacesRepository(appContext, api, BuildConfig.MAPY_API_KEY)
    }

    val authRepository by lazy { AuthRepository(appContext.getString(R.string.default_web_client_id)) }

    val backupRepository by lazy { BackupRepository(tripRepository, preferences) }

    val locationProvider = LocationProvider(appContext)

    val mapyApiKey: String = BuildConfig.MAPY_API_KEY
}
