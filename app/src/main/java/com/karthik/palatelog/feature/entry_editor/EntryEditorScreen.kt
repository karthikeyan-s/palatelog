package com.karthik.palatelog.feature.entry_editor

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.karthik.palatelog.core.designsystem.AmberPrimary
import com.karthik.palatelog.core.designsystem.DishVerdictBadge
import com.karthik.palatelog.core.designsystem.VerdictCautionAmber
import com.karthik.palatelog.core.designsystem.VerdictCautionAmberContainer
import com.karthik.palatelog.core.designsystem.VerdictGoGreen
import com.karthik.palatelog.core.designsystem.VerdictGoGreenContainer
import com.karthik.palatelog.core.designsystem.VerdictNoGoRed
import com.karthik.palatelog.core.designsystem.VerdictNoGoRedContainer
import com.karthik.palatelog.core.model.Channel
import com.karthik.palatelog.core.model.DishVerdict
import com.karthik.palatelog.core.model.Verdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    viewModel: EntryEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EntryEditorEvent.SavedSuccessfully -> {
                    Toast.makeText(context, "Visit & dish learnings saved! ✅", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is EntryEditorEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.restaurantId != null) "Log Visit to ${uiState.restaurantName}" else "Log Visit / Order",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Channel Selector
            Text(
                text = "HOW DID YOU ORDER / VISIT?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Channel.values().forEach { channel ->
                    FilterChip(
                        selected = uiState.channel == channel,
                        onClick = { viewModel.onChannelSelected(channel) },
                        label = { Text(channel.displayName) }
                    )
                }
            }

            // Section 2: Restaurant Name
            OutlinedTextField(
                value = uiState.restaurantName,
                onValueChange = { viewModel.onRestaurantNameChanged(it) },
                label = { Text("Restaurant Name *") },
                placeholder = { Text("e.g. Nagarjuna / Sri Krishna Grand") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Previous Dishes Chips for this restaurant
            if (uiState.previousDishes.isNotEmpty()) {
                Column {
                    Text(
                        text = "PAST DISHES REMEMBERED (TAP TO ADD):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.previousDishes.distinctBy { it.dishName }.forEach { dish ->
                            val verdict = try { DishVerdict.valueOf(dish.verdict) } catch (e: Exception) { DishVerdict.GO }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.addDishDraft(
                                            dishName = dish.dishName,
                                            verdict = verdict,
                                            tags = dish.tags.joinToString(", ")
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${dish.dishName} ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    DishVerdictBadge(verdict = verdict)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Overall Visit Verdict
            Text(
                text = "OVERALL RESTAURANT VERDICT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerdictChoiceChip(
                    label = "GO 🟢",
                    isSelected = uiState.overallVerdict == Verdict.GO,
                    color = VerdictGoGreen,
                    containerColor = VerdictGoGreenContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.GO) },
                    modifier = Modifier.weight(1f)
                )
                VerdictChoiceChip(
                    label = "NO-GO 🔴",
                    isSelected = uiState.overallVerdict == Verdict.NO_GO,
                    color = VerdictNoGoRed,
                    containerColor = VerdictNoGoRedContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.NO_GO) },
                    modifier = Modifier.weight(1f)
                )
                VerdictChoiceChip(
                    label = "CAUTION 🟡",
                    isSelected = uiState.overallVerdict == Verdict.CAUTION,
                    color = VerdictCautionAmber,
                    containerColor = VerdictCautionAmberContainer,
                    onClick = { viewModel.onVerdictSelected(Verdict.CAUTION) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Section 4: Dishes Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISHES / ITEMS ORDERED",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { viewModel.addDishDraft() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Dish")
                }
            }

            if (uiState.dishes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No dishes added yet. Tap \"Add Dish\" or tap a remembered dish above to isolate what was good vs soggy/bad!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                uiState.dishes.forEachIndexed { index, dishDraft ->
                    DishEditorCard(
                        index = index,
                        draft = dishDraft,
                        familyMembers = uiState.familyMembers,
                        onUpdate = { updated -> viewModel.updateDishDraft(index, updated) },
                        onRemove = { viewModel.removeDishDraft(index) }
                    )
                }
            }

            // Section 5: Bill & Details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = AmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bill & Notes (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = uiState.billAmountText,
                        onValueChange = { viewModel.onBillAmountChanged(it) },
                        label = { Text("Total Bill Amount (₹)") },
                        placeholder = { Text("e.g. 1250") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.generalNotes,
                        onValueChange = { viewModel.onGeneralNotesChanged(it) },
                        label = { Text("General Visit Remarks") },
                        placeholder = { Text("e.g. Slow service on weekend, clean AC dining hall") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 6: Shared with Family Toggle
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
                    Icon(Icons.Default.People, contentDescription = null, tint = AmberPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Share with Family Drive",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.isShared) "Syncs to Family Shared Drive" else "Kept private on this phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isShared,
                    onCheckedChange = { viewModel.onSharedToggle(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveEntry() },
                enabled = !uiState.isSaving && uiState.restaurantName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Save Visit & Dishes",
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
private fun DishEditorCard(
    index: Int,
    draft: DishDraft,
    familyMembers: List<com.karthik.palatelog.core.model.FamilyMember>,
    onUpdate: (DishDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dish #${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = { onUpdate(draft.copy(name = it)) },
                label = { Text("Dish Name *") },
                placeholder = { Text("e.g. Nagarjuna Biriyani / Chicken Fry") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Dish Verdict selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DishVerdict.values().forEach { v ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (draft.verdict == v) {
                                    when (v) {
                                        DishVerdict.GO -> VerdictGoGreenContainer
                                        DishVerdict.NO_GO -> VerdictNoGoRedContainer
                                        DishVerdict.MEH -> VerdictCautionAmberContainer
                                    }
                                } else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onUpdate(draft.copy(verdict = v)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = v.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (draft.verdict == v) {
                                when (v) {
                                    DishVerdict.GO -> VerdictGoGreen
                                    DishVerdict.NO_GO -> VerdictNoGoRed
                                    DishVerdict.MEH -> VerdictCautionAmber
                                }
                            } else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft.tagsText,
                onValueChange = { onUpdate(draft.copy(tagsText = it)) },
                label = { Text("Dish Remarks / Tags") },
                placeholder = { Text("e.g. Soggy during delivery, Fragrant rice") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Family Member Reaction Input
            OutlinedTextField(
                value = draft.memberReaction,
                onValueChange = { onUpdate(draft.copy(memberReaction = it)) },
                label = { Text("Who liked/disliked? (Family Notes)") },
                placeholder = { Text("e.g. Wife loved it, I disliked (too oily)") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VerdictChoiceChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
