package com.prismpixelvertex.palatelog.core.designsystem

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Clean composable map facade.
 * Isolates the underlying Android View (OSMDroid) from screen layouts so that a
 * multiplatform or iOS MapKit implementation can be plugged in without changing screen code.
 */
@Composable
fun PalateInteractiveMap(
    modifier: Modifier = Modifier,
    centerLatitude: Double,
    centerLongitude: Double,
    zoomLevel: Double = 16.0,
    markers: List<PalateMapMarker> = emptyList(),
    onSingleTap: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onLongPress: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onMarkerClick: ((markerId: String) -> Unit)? = null
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(zoomLevel)
            controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
        }
    }

    val eventsOverlay = remember(onSingleTap, onLongPress) {
        if (onSingleTap != null || onLongPress != null) {
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    onSingleTap?.invoke(p.latitude, p.longitude)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    onLongPress?.invoke(p.latitude, p.longitude)
                    return true
                }
            })
        } else {
            null
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()
            eventsOverlay?.let { map.overlays.add(it) }

            markers.forEach { markerData ->
                val marker = Marker(map).apply {
                    id = markerData.id
                    position = GeoPoint(markerData.latitude, markerData.longitude)
                    title = markerData.title
                    snippet = markerData.snippet
                    markerData.customIcon?.let { icon = it }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { m, _ ->
                        onMarkerClick?.invoke(m.id)
                        true
                    }
                }
                map.overlays.add(marker)
            }
            map.invalidate()
        }
    )
}
