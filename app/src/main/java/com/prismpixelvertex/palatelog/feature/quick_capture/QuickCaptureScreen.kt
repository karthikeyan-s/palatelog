package com.prismpixelvertex.palatelog.feature.quick_capture

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
import com.prismpixelvertex.palatelog.core.designsystem.LocationPickerDialog
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmber
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmberContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreenContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRedContainer
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.feature.entry_editor.PhotoAttachmentBox

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
                            text = stringResource(R.string.quick_capture_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        var showMapPicker by remember { mutableStateOf(false) }

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
                onUseNearby = { viewModel.onUseNearbyRestaurant(it) },
                onOpenMapPicker = { showMapPicker = true }
            )

            if (showMapPicker) {
                LocationPickerDialog(
                    initialLatitude = uiState.latitude,
                    initialLongitude = uiState.longitude,
                    onDismiss = { showMapPicker = false },
                    onLocationConfirmed = { lat, lng ->
                        viewModel.onLocationPicked(lat, lng)
                        showMapPicker = false
                    }
                )
            }

            // Step 1: Big Verdict Selector
            Text(
                text = stringResource(R.string.quick_capture_verdict_header),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerdictChoiceButton(
                    label = stringResource(R.string.verdict_no_go),
                    sublabel = "Never Again!",
                    icon = Icons.Default.Block,
                    isSelected = uiState.selectedVerdict == Verdict.NO_GO,
                    selectedColor = VerdictNoGoRed,
                    containerColor = VerdictNoGoRedContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.NO_GO) },
                    modifier = Modifier.weight(1f)
                )

                VerdictChoiceButton(
                    label = stringResource(R.string.verdict_go),
                    sublabel = "Must Visit!",
                    icon = Icons.Default.CheckCircle,
                    isSelected = uiState.selectedVerdict == Verdict.GO,
                    selectedColor = VerdictGoGreen,
                    containerColor = VerdictGoGreenContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.GO) },
                    modifier = Modifier.weight(1f)
                )

                VerdictChoiceButton(
                    label = stringResource(R.string.verdict_caution),
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
                label = { Text(stringResource(R.string.quick_capture_spot_name_hint)) },
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
                    label = { Text(stringResource(R.string.quick_capture_reason_header)) },
                    placeholder = { Text("e.g. Terrible watery tea / Soggy side") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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

            // Photo Attachment (Optional)
            Text(
                text = "ATTACH PHOTO (OPTIONAL)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoAttachmentBox(
                photoUris = uiState.photoUris,
                onPhotoAdded = { uri -> viewModel.onPhotoAdded(uri) },
                onPhotoRemoved = { uri -> viewModel.onPhotoRemoved(uri) }
            )

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
                shape = RoundedCornerShape(Dimens.RadiusLarge),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeightStandard)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.quick_capture_save_button),
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
    onUseNearby: (com.prismpixelvertex.palatelog.core.database.RestaurantEntity) -> Unit,
    onOpenMapPicker: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (uiState.isLocationLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Acquiring GPS location...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val locText = listOfNotNull(
                            uiState.streetOrArea.ifBlank { null },
                            uiState.city.ifBlank { null }
                        ).joinToString(", ")
                        Text(
                            text = locText.ifBlank { "GPS Location Acquired" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                TextButton(onClick = onOpenMapPicker) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change on Map", fontSize = 12.sp)
                }
            }

            uiState.nearbyExistingRestaurant?.let { nearby ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Are you at ${nearby.name}?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap to reuse this spot",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    TextButton(onClick = { onUseNearby(nearby) }) {
                        Text("Use This", fontWeight = FontWeight.Bold)
                    }
                }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) selectedColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
