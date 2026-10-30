package com.prismpixelvertex.palatelog.core.designsystem

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prismpixelvertex.palatelog.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun LocationPickerDialog(
    initialLatitude: Double,
    initialLongitude: Double,
    onDismiss: () -> Unit,
    onLocationConfirmed: (latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current

    // Default fallback coordinates if invalid (Bangalore default or given initial)
    val startLat = if (initialLatitude != 0.0) initialLatitude else 12.9716
    val startLng = if (initialLongitude != 0.0) initialLongitude else 77.5946

    var selectedPoint by remember { mutableStateOf(GeoPoint(startLat, startLng)) }

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            controller.setCenter(selectedPoint)
        }
    }

    val eventsOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                selectedPoint = p
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                selectedPoint = p
                return true
            }
        })
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(Dimens.RadiusDialog),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationHigh),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(Dimens.LocationDialogHeight)
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.SpacingDefault)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(Dimens.IconSizeDefault)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                        Column {
                            Text(
                                text = stringResource(R.string.location_picker_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.location_picker_instructions),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Map View Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxWidth(),
                        update = { map ->
                            map.overlays.clear()
                            map.overlays.add(eventsOverlay)

                            val marker = Marker(map).apply {
                                position = selectedPoint
                                title = context.getString(R.string.restaurant_detail_selected_spot_default)
                                snippet = context.getString(R.string.map_tap_actions_sub)
                                icon = MapMarkerFactory.getCustomLocationMarker(context)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            map.overlays.add(marker)
                            map.invalidate()
                        }
                    )

                    // Re-center on pin button
                    FloatingActionButton(
                        onClick = {
                            mapView.controller.animateTo(selectedPoint)
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.location_picker_my_location), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Coordinates readout
                Text(
                    text = stringResource(R.string.restaurant_detail_coords_fmt, String.format(Locale.US, "%.5f", selectedPoint.latitude), String.format(Locale.US, "%.5f", selectedPoint.longitude)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            onLocationConfirmed(selectedPoint.latitude, selectedPoint.longitude)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.location_picker_confirm), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
