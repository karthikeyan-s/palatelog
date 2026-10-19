package com.karthik.palatelog.feature.quick_capture

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.karthik.palatelog.core.designsystem.VerdictCautionAmber
import com.karthik.palatelog.core.designsystem.VerdictCautionAmberContainer
import com.karthik.palatelog.core.designsystem.VerdictGoGreen
import com.karthik.palatelog.core.designsystem.VerdictGoGreenContainer
import com.karthik.palatelog.core.designsystem.VerdictNoGoRed
import com.karthik.palatelog.core.designsystem.VerdictNoGoRedContainer
import com.karthik.palatelog.core.model.Verdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureScreen(
    viewModel: QuickCaptureViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickCaptureEvent.SavedSuccessfully -> {
                    Toast.makeText(context, "Logged in 3 seconds! 🎯", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is QuickCaptureEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = VerdictNoGoRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3-Second Quick Log",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Badge & Nearby Suggestion
            LocationHeaderCard(
                uiState = uiState,
                onUseNearby = { viewModel.onUseNearbyRestaurant(it) }
            )

            // Step 1: Big Verdict Selector
            Text(
                text = "1. WHAT IS YOUR VERDICT?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerdictChoiceButton(
                    label = "NO-GO 🔴",
                    sublabel = "Never Again!",
                    icon = Icons.Default.Block,
                    isSelected = uiState.selectedVerdict == Verdict.NO_GO,
                    selectedColor = VerdictNoGoRed,
                    containerColor = VerdictNoGoRedContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.NO_GO) },
                    modifier = Modifier.weight(1f)
                )

                VerdictChoiceButton(
                    label = "GO 🟢",
                    sublabel = "Must Visit!",
                    icon = Icons.Default.CheckCircle,
                    isSelected = uiState.selectedVerdict == Verdict.GO,
                    selectedColor = VerdictGoGreen,
                    containerColor = VerdictGoGreenContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.GO) },
                    modifier = Modifier.weight(1f)
                )

                VerdictChoiceButton(
                    label = "CAUTION 🟡",
                    sublabel = "Mixed / Okay",
                    icon = Icons.Default.Warning,
                    isSelected = uiState.selectedVerdict == Verdict.CAUTION,
                    selectedColor = VerdictCautionAmber,
                    containerColor = VerdictCautionAmberContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.CAUTION) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Step 2: Name Input
            OutlinedTextField(
                value = uiState.placeName,
                onValueChange = { viewModel.onPlaceNameChanged(it) },
                label = { Text("Restaurant / Stall Name *") },
                placeholder = { Text("e.g. Highway Tea Point / Nagarjuna") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Step 3: Dish Name (Optional)
            OutlinedTextField(
                value = uiState.dishName,
                onValueChange = { viewModel.onDishNameChanged(it) },
                label = { Text("Dish / Item (Optional)") },
                placeholder = { Text("e.g. Masala Tea / Chicken Fry") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Step 4: Quick Reason / Tag with one-tap suggestions
            Column {
                OutlinedTextField(
                    value = uiState.quickReason,
                    onValueChange = { viewModel.onQuickReasonChanged(it) },
                    label = { Text("Quick Reason / Learning") },
                    placeholder = { Text("e.g. Terrible watery tea / Soggy side") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Suggestion chips
                val quickChips = if (uiState.selectedVerdict == Verdict.NO_GO) {
                    listOf("Watery tea", "Soggy food", "Dirty restrooms", "Overpriced", "Stale food", "Too oily")
                } else {
                    listOf("Super crispy", "Fragrant spices", "Clean restrooms", "Great service", "Value for money")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickChips.forEach { chipText ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.onQuickReasonChanged(chipText) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Highway Travel Stop Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Highway Outstation Stop",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Save for road trip memory trail",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isHighwayStop,
                    onCheckedChange = { viewModel.onHighwayToggle(it) }
                )
            }

            if (uiState.isHighwayStop) {
                OutlinedTextField(
                    value = uiState.highwayName,
                    onValueChange = { viewModel.onHighwayNameChanged(it) },
                    label = { Text("Highway / Corridor Name") },
                    placeholder = { Text("e.g. NH44 / Bangalore-Chennai") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveQuickLog() },
                enabled = !uiState.isSaving && uiState.placeName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.selectedVerdict) {
                        Verdict.NO_GO -> VerdictNoGoRed
                        Verdict.GO -> VerdictGoGreen
                        Verdict.CAUTION -> VerdictCautionAmber
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save & Finish (3s)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationHeaderCard(
    uiState: QuickCaptureUiState,
    onUseNearby: (com.karthik.palatelog.core.database.RestaurantEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (uiState.isLocationLoading) MaterialTheme.colorScheme.outline else VerdictGoGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (uiState.isLocationLoading) {
                Text(
                    text = "Acquiring GPS location...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val locText = listOfNotNull(
                    uiState.streetOrArea.ifBlank { null },
                    uiState.city.ifBlank { null }
                ).joinToString(", ")
                Text(
                    text = if (locText.isNotBlank()) "📍 $locText" else "📍 Location Locked (${String.format("%.4f", uiState.latitude)}, ${String.format("%.4f", uiState.longitude)})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (uiState.nearbyExistingRestaurant != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onUseNearby(uiState.nearbyExistingRestaurant) }
                    .padding(8.dp)
            ) {
                Text(
                    text = "Found nearby spot: \"${uiState.nearbyExistingRestaurant.name}\" - Tap to reuse",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun VerdictChoiceButton(
    label: String,
    sublabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sublabel,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
