package com.prismpixelvertex.palatelog.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.VerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.location.GeoUtils
import com.prismpixelvertex.palatelog.core.navigation.ExternalMapNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRestaurantDetail: (String) -> Unit,
    onNavigateToQuickCapture: () -> Unit,
    onNavigateToEntryEditor: (restaurantId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PalateLog",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your food learnings & memory log",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // The 3-Second Quick Regret Logger (Speed Dial)
                ExtendedFloatingActionButton(
                    onClick = onNavigateToQuickCapture,
                    containerColor = VerdictNoGoRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                    text = { Text("Quick Regret Log", fontWeight = FontWeight.Bold) }
                )

                // Standard Full Add Button
                FloatingActionButton(
                    onClick = { onNavigateToEntryEditor(null) },
                    containerColor = AmberPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Visit / Order")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search places, cuisines, dishes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.ALL,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.ALL) },
                    label = { Text("All (${uiState.totalCount})") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.GO_ONLY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.GO_ONLY) },
                    label = { Text("🟢 Go (${uiState.goCount})") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.NO_GO_ONLY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.NO_GO_ONLY) },
                    label = { Text("🔴 No-Go (${uiState.noGoCount})") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.NEARBY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.NEARBY) },
                    label = { Text("📍 Near Me (<15km)") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.HIGHWAY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.HIGHWAY) },
                    label = { Text("🛣️ Highway") }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.DELIVERY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.DELIVERY) },
                    label = { Text("🛵 Zomato / Swiggy") }
                )
            }

            // Restaurant Cards List
            if (uiState.restaurants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "No restaurant entries yet" else "No matching spots found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the Quick Regret Log or + button to record your first food insight!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.restaurants, key = { it.restaurant.id }) { item ->
                        RestaurantCard(
                            item = item,
                            onClick = { onNavigateToRestaurantDetail(item.restaurant.id) },
                            onNavigateClick = {
                                ExternalMapNavigator.navigateTo(
                                    context = context,
                                    latitude = item.restaurant.latitude,
                                    longitude = item.restaurant.longitude,
                                    label = item.restaurant.name
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestaurantCard(
    item: RestaurantCardUiState,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    val r = item.restaurant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Name + Verdict Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = r.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                VerdictBadge(verdict = r.overallVerdict)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Row: Cuisine & Type
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (r.cuisine.isNotBlank()) {
                    Text(
                        text = r.cuisine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    text = r.restaurantType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Location & Highway Row
            if (r.highwayOrRoute.isNotBlank() || r.address.isNotBlank() || r.city.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val locationText = buildString {
                    if (r.highwayOrRoute.isNotBlank()) append("🛣️ ${r.highwayOrRoute} • ")
                    if (r.city.isNotBlank()) append(r.city)
                    else if (r.address.isNotBlank()) append(r.address)
                }
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Distance + Visits + Navigate Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.distanceKm != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = GeoUtils.formatDistance(item.distanceKm),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "${item.totalVisits} visit${if (item.totalVisits == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Navigate in Google Maps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
