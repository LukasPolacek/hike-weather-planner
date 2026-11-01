package cz.polacek.hikeweather.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.core.network.MapyApi
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Bod na mape, kam se ma mapa presunout (zmena objektu = nova animace) */
data class MapTarget(val latitude: Double, val longitude: Double, val zoom: Double = 13.0)

/**
 * Turisticka mapa Mapy.com zobrazena pres MapLibre
 * vybrany bod je vzdy uprostred mapy (znacka se nehybe, posouva se mapa)
 *
 * @param interactive false = mapa jen k nahledu, bez posouvani (napr. v kartach)
 * @param onCenterChanged zavola se po dokonceni posunu mapy se souradnicemi stredu
 */
@Composable
fun MapyMap(
    apiKey: String,
    target: MapTarget,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    showCenterPin: Boolean = true,
    onCenterChanged: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnCenterChanged by rememberUpdatedState(onCenterChanged)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    // MapView potrebuje dostavat udalosti zivotniho cyklu obrazovky
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = {
                mapView.getMapAsync { libreMap ->
                    libreMap.setStyle(Style.Builder().fromJson(styleJson(apiKey)))
                    libreMap.uiSettings.apply {
                        isLogoEnabled = false
                        isAttributionEnabled = false // atribuce Mapy.com je zobrazena vlastni
                        setAllGesturesEnabled(interactive)
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                    }
                    libreMap.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(target.latitude, target.longitude))
                        .zoom(target.zoom)
                        .build()
                    libreMap.addOnCameraIdleListener {
                        libreMap.cameraPosition.target?.let { center ->
                            currentOnCenterChanged?.invoke(center.latitude, center.longitude)
                        }
                    }
                    map = libreMap
                }
                mapView
            },
            modifier = Modifier.matchParentSize()
        )

        if (showCenterPin) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .offset(y = (-20).dp) // spicka znacky presne na stredu mapy
            )
        }

        MapyAttribution(Modifier.align(Alignment.BottomStart).padding(8.dp))
    }

    // presun mapy na novy cil (napr. po vyberu mista ve vyhledavani)
    LaunchedEffect(map, target) {
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), target.zoom),
            600
        )
    }
}

/**
 * Logo a autorska prava Mapy.com - podminka pouziti mapovych dlazdic
 * oboji odkazuje na stranky Mapy.com
 */
@Composable
fun MapyAttribution(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.mapy_logo),
            contentDescription = "Mapy.com",
            modifier = Modifier
                .height(20.dp)
                .clickable { open("https://mapy.com/") }
        )
        Text(
            text = "  © Seznam.cz a.s. a další ",
            color = Color(0xFF333333),
            fontSize = 10.sp,
            modifier = Modifier.clickable { open("https://api.mapy.com/copyright") }
        )
    }
}

/** styl mapy: jedna rastrova vrstva s turistickou mapou Mapy.com */
private fun styleJson(apiKey: String): String {
    val source = if (apiKey.isBlank()) "" else """
        "sources": {
          "mapy": { "type": "raster", "url": "${MapyApi.outdoorTilesJson(apiKey)}", "tileSize": 256 }
        },
    """.trimIndent()
    val layer = if (apiKey.isBlank()) "" else """, { "id": "mapy", "type": "raster", "source": "mapy" }"""
    return """
        {
          "version": 8,
          ${if (source.isEmpty()) "\"sources\": {}," else source}
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#E8E4DA" } }$layer
          ]
        }
    """.trimIndent()
}
