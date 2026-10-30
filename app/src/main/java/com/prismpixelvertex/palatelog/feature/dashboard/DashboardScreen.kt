package com.prismpixelvertex.palatelog.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
import com.prismpixelvertex.palatelog.core.designsystem.DishVerdictBadge
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
    onNavigateToHelpGuide: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToHelpGuide(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.help_title),
                            tint = AmberPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Quick Regret Log (3s) FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                onNavigateToQuickCapture()
                            },
                            containerColor = VerdictNoGoRed,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                            text = { Text(stringResource(R.string.dashboard_fab_quick_log), fontWeight = FontWeight.Bold) }
                        )

                        // 2. Full Visit Log FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                onNavigateToEntryEditor(null)
                            },
                            containerColor = AmberPrimary,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text(stringResource(R.string.dashboard_fab_full_visit), fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Main Trigger FAB
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = if (isFabExpanded) MaterialTheme.colorScheme.surfaceVariant else AmberPrimary,
                    contentColor = if (isFabExpanded) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isFabExpanded) 45f else 0f,
                        label = "fab_rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isFabExpanded) stringResource(R.string.action_close) else stringResource(R.string.nav_quick_log),
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.showFirstLaunchHelpDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onDismissFirstLaunchHelp() },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(Dimens.IconSizeLarge)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.help_first_launch_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.help_first_launch_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onAcceptFirstLaunchHelp()
                            onNavigateToHelpGuide(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.help_first_launch_action_view),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDismissFirstLaunchHelp() }) {
                        Text(
                            text = stringResource(R.string.help_first_launch_action_dismiss),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                shape = RoundedCornerShape(Dimens.RadiusDialog)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.dashboard_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_close))
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
                    label = { Text(stringResource(R.string.dashboard_filter_all, uiState.totalCount)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.GO_ONLY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.GO_ONLY) },
                    label = { Text(stringResource(R.string.dashboard_filter_go, uiState.goCount)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.NO_GO_ONLY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.NO_GO_ONLY) },
                    label = { Text(stringResource(R.string.dashboard_filter_no_go, uiState.noGoCount)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.NEARBY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.NEARBY) },
                    label = { Text(stringResource(R.string.dashboard_filter_nearby)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.HIGHWAY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.HIGHWAY) },
                    label = { Text(stringResource(R.string.dashboard_filter_highway)) }
                )
                FilterChip(
                    selected = uiState.selectedFilter == DashboardFilter.DELIVERY,
                    onClick = { viewModel.onFilterSelected(DashboardFilter.DELIVERY) },
                    label = { Text(stringResource(R.string.dashboard_filter_delivery)) }
                )
            }

            // Restaurant Cards List
            if (uiState.restaurants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.SpacingXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(Dimens.IconSizeHero)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingDefault))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) stringResource(R.string.dashboard_empty_title) else stringResource(R.string.dashboard_empty_search_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                        Text(
                            text = stringResource(R.string.dashboard_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
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

            // Matched Dishes or Top Dishes Row
            if (item.matchedDishes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.matchedDishes.take(4).forEach { dish ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DishVerdictBadge(verdict = dish.verdict)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dish.dishName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (item.topMustTryDishes.isNotEmpty() || item.topAvoidDishes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.topMustTryDishes.isNotEmpty()) {
                        Text(
                            text = "🟢 Must Try: ${item.topMustTryDishes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VerdictGoGreen,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (item.topAvoidDishes.isNotEmpty()) {
                        Text(
                            text = "🔴 Avoid: ${item.topAvoidDishes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VerdictNoGoRed,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
                        text = stringResource(
                            if (item.totalVisits == 1) R.string.dashboard_card_visits else R.string.dashboard_card_visits_plural,
                            item.totalVisits
                        ),
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
                        contentDescription = stringResource(R.string.map_preview_navigate),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
