package cz.polacek.hikeweather.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Rozhrani pro REST API Mapy.com
 * naseptavani mist pri psani a prevod souradnic na adresu
 */
interface MapyApi {

    @GET("v1/suggest")
    suspend fun suggest(
        @Query("query") query: String,
        @Query("apikey") apiKey: String,
        @Query("lang") lang: String = "cs",
        @Query("limit") limit: Int = 6,
        // jen obce, casti obci, ulice, adresy a body zajmu (vrcholy, rozhledny, chaty...)
        @Query("type") types: List<String> = listOf(
            "regional.municipality",
            "regional.municipality_part",
            "regional.street",
            "regional.address",
            "poi"
        )
    ): MapyResponseDto

    @GET("v1/rgeocode")
    suspend fun reverseGeocode(
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("apikey") apiKey: String,
        @Query("lang") lang: String = "cs"
    ): MapyResponseDto

    companion object {
        const val BASE_URL = "https://api.mapy.com/"

        /** TileJSON turisticke mapy - MapLibre si z nej vezme adresy dlazdic i atribuci */
        fun outdoorTilesJson(apiKey: String) = "${BASE_URL}v1/maptiles/outdoor/tiles.json?apikey=$apiKey"
    }
}

data class MapyResponseDto(
    @SerializedName("items") val items: List<MapyItemDto>?
)

data class MapyItemDto(
    @SerializedName("name") val name: String,
    // napr. "regional.address", "poi"
    @SerializedName("type") val type: String?,
    // typ mista slovne, napr. "Obec", "Rozhledna"
    @SerializedName("label") val label: String?,
    // okoli mista, napr. "Olomouc, Olomoucký kraj, Česko"
    @SerializedName("location") val location: String?,
    @SerializedName("position") val position: MapyPositionDto
)

data class MapyPositionDto(
    @SerializedName("lon") val lon: Double,
    @SerializedName("lat") val lat: Double
)
