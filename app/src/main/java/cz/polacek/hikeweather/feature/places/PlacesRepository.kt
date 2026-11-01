package cz.polacek.hikeweather.feature.places

import android.content.Context
import android.location.Geocoder
import cz.polacek.hikeweather.core.network.MapyApi
import cz.polacek.hikeweather.core.network.MapyItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Misto nalezene vyhledavanim nebo na mape */
data class Place(
    val name: String,
    // upresneni, napr. "Rozhledna · Olomoucký kraj"
    val detail: String,
    val latitude: Double,
    val longitude: Double
)

interface PlacesRepository {
    /** naseptavani mist podle zadaneho textu */
    suspend fun suggest(query: String): Result<List<Place>>

    /** nazev mista pro souradnice (napr. po posunuti mapy) */
    suspend fun placeAt(lat: Double, lon: Double): Result<Place>
}

/**
 * Vyhledavani pres Mapy.com, ktere dobre zna turisticke cile (vrcholy, chaty, rozhledny)
 * pri chybe nebo chybejicim klici se pouzije systemovy Geocoder Androidu
 */
class MapyPlacesRepository(
    private val context: Context,
    private val api: MapyApi,
    private val apiKey: String
) : PlacesRepository {

    override suspend fun suggest(query: String): Result<List<Place>> {
        if (query.isBlank()) return Result.success(emptyList())
        if (apiKey.isNotBlank()) {
            val mapy = runCatching { api.suggest(query.trim(), apiKey).items.orEmpty().map { it.toPlace() } }
            if (mapy.isSuccess) return mapy
        }
        return geocoderSearch(query)
    }

    override suspend fun placeAt(lat: Double, lon: Double): Result<Place> {
        if (apiKey.isNotBlank()) {
            val mapy = runCatching {
                api.reverseGeocode(lon, lat, apiKey).items?.firstOrNull()?.toPlace()?.copy(latitude = lat, longitude = lon)
            }
            mapy.getOrNull()?.let { return Result.success(it) }
        }
        return geocoderReverse(lat, lon)
    }

    private fun MapyItemDto.toPlace() = Place(
        name = name,
        // u adres, ulic a obci je label obecny ("Adresa"), zobrazi se jen u bodu zajmu (napr. "Rozhledna")
        detail = listOfNotNull(label.takeIf { type?.startsWith("regional") != true }, location)
            .filter { it.isNotBlank() }
            .joinToString(" · "),
        latitude = position.lat,
        longitude = position.lon
    )

    @Suppress("DEPRECATION")
    private suspend fun geocoderSearch(query: String): Result<List<Place>> = withContext(Dispatchers.IO) {
        runCatching {
            Geocoder(context, Locale.getDefault()).getFromLocationName(query, 5).orEmpty().map { address ->
                Place(
                    name = address.featureName ?: query,
                    detail = address.getAddressLine(0).orEmpty(),
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocoderReverse(lat: Double, lon: Double): Result<Place> = withContext(Dispatchers.IO) {
        runCatching {
            val address = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
            Place(
                name = address?.locality ?: address?.featureName ?: formatCoordinates(lat, lon),
                detail = address?.getAddressLine(0).orEmpty(),
                latitude = lat,
                longitude = lon
            )
        }
    }
}

fun formatCoordinates(lat: Double, lon: Double): String =
    String.format(Locale.US, "%.4f, %.4f", lat, lon)
