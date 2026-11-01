package cz.polacek.hikeweather.feature.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cz.polacek.hikeweather.HikeWeatherApp
import cz.polacek.hikeweather.feature.weather.ForecastSummarizer
import cz.polacek.hikeweather.feature.weather.WeatherAlerts
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Pravidelna kontrola predpovedi pro vylety v pristich 5 dnech (dal predpoved nesaha)
 * WorkManager ulohu spousti i po restartu telefonu a jen kdyz je pripojeni k internetu
 * pri novem zhorseni pocasi posle notifikaci
 */
class WeatherCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as HikeWeatherApp).container
        if (!container.preferences.weatherAlertsEnabled.value) return Result.success()

        val today = LocalDate.now()
        val upcoming = container.tripRepository.getTrips().filter { trip ->
            val date = trip.plannedDate ?: return@filter false
            !date.isBefore(today) && !date.isAfter(today.plusDays(FORECAST_DAYS))
        }

        for (trip in upcoming) {
            val date = trip.plannedDate ?: continue
            val days = container.weatherRepository.dailyForecast(trip.latitude, trip.longitude)
                .getOrElse { return Result.retry() }
            val day = ForecastSummarizer.forDate(days, date) ?: continue
            val alert = WeatherAlerts.evaluate(day, trip.forecastMaxTemperature)

            container.tripRepository.save(
                trip.copy(
                    weatherSummary = day.summary,
                    forecastMaxTemperature = day.maxTemperature,
                    alertTitle = alert?.type?.title,
                    alertDetail = alert?.detail,
                    forecastCheckedAt = System.currentTimeMillis()
                )
            )

            // upozornit jen na nove nebo jine upozorneni, ne pri kazde kontrole znovu
            if (alert != null && alert.type.title != trip.alertTitle) {
                Notifications.showWeatherAlert(applicationContext, trip.id, trip.name, alert.type.title, alert.detail)
            }
        }
        return Result.success()
    }

    companion object {
        private const val FORECAST_DAYS = 5L
        private const val PERIODIC_WORK = "weather_check"
        private const val IMMEDIATE_WORK = "weather_check_now"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** pravidelna kontrola kazde 3 hodiny */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherCheckWorker>(3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** okamzita kontrola, napr. po ulozeni noveho vyletu */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}
