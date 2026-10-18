package com.karthik.palatelog.feature.restaurant_detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.karthik.palatelog.core.designsystem.AmberPrimary
import com.karthik.palatelog.core.designsystem.ChannelBadge
import com.karthik.palatelog.core.designsystem.DishVerdictBadge
import com.karthik.palatelog.core.designsystem.FamilyReactionTag
import com.karthik.palatelog.core.designsystem.VerdictBadge
import com.karthik.palatelog.core.designsystem.VerdictCautionAmber
import com.karthik.palatelog.core.designsystem.VerdictGoGreen
import com.karthik.palatelog.core.designsystem.VerdictNoGoRed
import com.karthik.palatelog.core.model.DishFeedback
import com.karthik.palatelog.core.model.VisitEntry
import com.karthik.palatelog.core.navigation.ExternalMapNavigator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    viewModel: RestaurantDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogVisit: (restaurantId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val restaurant = uiState.restaurant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurant?.name ?: "Restaurant Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteRestaurant()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading || restaurant == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AmberPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            VerdictBadge(verdict = restaurant.overallVerdict)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${restaurant.restaurantType.displayName} • ${restaurant.cuisine.ifBlank { "General Dining" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (restaurant.highwayOrRoute.isNotBlank() || restaurant.address.isNotBlank() || restaurant.city.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val fullLoc = listOfNotNull(
                                if (restaurant.highwayOrRoute.isNotBlank()) "🛣️ ${restaurant.highwayOrRoute}" else null,
                                restaurant.address.ifBlank { null },
                                restaurant.city.ifBlank { null }
                            ).joinToString(" • ")
                            Text(
                                text = fullLoc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    ExternalMapNavigator.navigateTo(
                                        context = context,
                                        latitude = restaurant.latitude,
                                        longitude = restaurant.longitude,
                                        label = restaurant.name
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate")
                            }

                            OutlinedButton(
                                onClick = { onNavigateToLogVisit(restaurant.id) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Visit")
                            }
                        }
                    }
                }

                // Section: Dish Insights Matrix
                Text(
                    text = "DISH INSIGHTS & MEMORY MATRIX",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.mustTryDishes.isEmpty() && uiState.avoidDishes.isEmpty() && uiState.neutralDishes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No individual dishes logged yet. Log a new visit to record specific dishes (e.g. Biriyani vs soggy sides).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 🔴 AVOID DISHES (Shown first to prevent repeats!)
                    if (uiState.avoidDishes.isNotEmpty()) {
                        DishVerdictGroup(
                            title = "AVOID NEXT TIME 🔴",
                            dishes = uiState.avoidDishes,
                            accentColor = VerdictNoGoRed
                        )
                    }

                    // 🟢 MUST TRY DISHES
                    if (uiState.mustTryDishes.isNotEmpty()) {
                        DishVerdictGroup(
                            title = "FAMILY FAVORITES & MUST TRY 🟢",
                            dishes = uiState.mustTryDishes,
                            accentColor = VerdictGoGreen
                        )
                    }

                    // 🟡 MEH / OK DISHES
                    if (uiState.neutralDishes.isNotEmpty()) {
                        DishVerdictGroup(
                            title = "NEUTRAL / OKAY DISHES 🟡",
                            dishes = uiState.neutralDishes,
                            accentColor = VerdictCautionAmber
                        )
                    }
                }

                // Section: Past Visits History
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VISIT HISTORY (${uiState.visits.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.visits.isEmpty()) {
                    Text(
                        text = "No visits logged yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.visits.forEach { visit ->
                        VisitHistoryCard(visit = visit)
                    }
                }
            }
        }
    }
}

@Composable
private fun DishVerdictGroup(
    title: String,
    dishes: List<DishFeedback>,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            dishes.forEach { dish ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dish.dishName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        DishVerdictBadge(verdict = dish.verdict)
                    }

                    if (dish.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Learning: ${dish.tags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (dish.memberPreferences.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dish.memberPreferences.forEach { (member, reaction) ->
                                FamilyReactionTag(memberName = member, reaction = reaction)
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun VisitHistoryCard(visit: VisitEntry) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = dateFormatter.format(Date(visit.visitDate))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelBadge(channel = visit.channel)
                VerdictBadge(verdict = visit.visitVerdict)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (visit.billAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Bill: ₹${String.format("%.0f", visit.billAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (visit.generalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${visit.generalNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
