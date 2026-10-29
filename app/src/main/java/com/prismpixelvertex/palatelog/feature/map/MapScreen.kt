package com.prismpixelvertex.palatelog.feature.map

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.MapMarkerFactory
import com.prismpixelvertex.palatelog.core.designsystem.VerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.navigation.ExternalMapNavigator
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToRestaurantDetail: (String) -> Unit,
    onNavigateToQuickCapture: (lat: Double, lng: Double, address: String, city: String) -> Unit,
    onNavigateToEntryEditor: (restaurantId: String?, lat: Double?, lng: Double?, address: String?, city: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(uiState.userLatitude, uiState.userLongitude))
        }
    }

    val eventsOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                viewModel.onMapTapped(p.latitude, p.longitude)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                viewModel.onMapTapped(p.latitude, p.longitude)
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()
                map.overlays.add(eventsOverlay)

                // Add User Location Marker
                if (uiState.isLocationReady) {
                    val userMarker = Marker(map).apply {
                        position = GeoPoint(uiState.userLatitude, uiState.userLongitude)
                        title = "Your Location"
                        icon = MapMarkerFactory.getUserLocationMarker(context)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(userMarker)
                }

                // Add Selected Custom Location Marker
                uiState.selectedCustomLocation?.let { customLoc ->
                    val customMarker = Marker(map).apply {
                        position = GeoPoint(customLoc.latitude, customLoc.longitude)
                        title = if (customLoc.street.isNotBlank()) customLoc.street else "Selected Spot 📍"
                        snippet = "Tap actions below to log"
                        icon = MapMarkerFactory.getCustomLocationMarker(context)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(customMarker)
                }

                // Add Restaurant Markers
                uiState.restaurants.forEach { r ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(r.latitude, r.longitude)
                        title = r.name
                        snippet = "${r.overallVerdict.name} • ${r.cuisine}"
                        icon = MapMarkerFactory.getRestaurantMarker(context, r.overallVerdict, r.restaurantType)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            viewModel.onMarkerSelected(r)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            }
        )

        // Center on Me FAB
        FloatingActionButton(
            onClick = {
                viewModel.loadLocation()
                mapView.controller.animateTo(GeoPoint(uiState.userLatitude, uiState.userLongitude))
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.location_picker_my_location))
        }

        // Bottom Card when a custom location on map is tapped
        AnimatedVisibility(
            visible = uiState.selectedCustomLocation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            uiState.selectedCustomLocation?.let { customLoc ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (customLoc.isLoadingAddress) "Acquiring Address..."
                                    else listOfNotNull(customLoc.street.ifBlank { null }, customLoc.city.ifBlank { null })
                                        .joinToString(", ").ifBlank { "Selected Location Spot" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearCustomLocation() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📍 Coordinates: ${String.format(java.util.Locale.US, "%.4f", customLoc.latitude)}, ${String.format(java.util.Locale.US, "%.4f", customLoc.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onNavigateToQuickCapture(
                                        customLoc.latitude,
                                        customLoc.longitude,
                                        customLoc.street,
                                        customLoc.city
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdictNoGoRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.dashboard_fab_quick_log))
                            }

                            Button(
                                onClick = {
                                    onNavigateToEntryEditor(
                                        null,
                                        customLoc.latitude,
                                        customLoc.longitude,
                                        customLoc.street,
                                        customLoc.city
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.dashboard_fab_full_visit))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Preview Card when a restaurant marker is tapped
        AnimatedVisibility(
            visible = uiState.selectedRestaurant != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            uiState.selectedRestaurant?.let { restaurant ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = restaurant.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onMarkerSelected(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerdictBadge(verdict = restaurant.overallVerdict)
                            if (uiState.selectedDistanceKm != null) {
                                Text(
                                    text = "• ${GeoUtils.formatDistance(uiState.selectedDistanceKm!!)} away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (restaurant.highwayOrRoute.isNotBlank() || restaurant.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (restaurant.highwayOrRoute.isNotBlank()) "🛣️ ${restaurant.highwayOrRoute}" else restaurant.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onNavigateToEntryEditor(
                                        restaurant.id,
                                        restaurant.latitude,
                                        restaurant.longitude,
                                        restaurant.address,
                                        restaurant.city
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.detail_action_new_visit))
                            }

                            OutlinedButton(
                                onClick = {
                                    ExternalMapNavigator.navigateTo(
                                        context = context,
                                        latitude = restaurant.latitude,
                                        longitude = restaurant.longitude,
                                        label = restaurant.name
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.detail_action_navigate))
                            }

                            OutlinedButton(
                                onClick = { onNavigateToRestaurantDetail(restaurant.id) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.map_preview_view_details))
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
