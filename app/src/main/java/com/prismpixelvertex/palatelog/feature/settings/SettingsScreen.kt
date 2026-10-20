package com.prismpixelvertex.palatelog.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.prismpixelvertex.palatelog.BuildConfig
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importBackupFromUri(context, it)
            showImportDialog = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.JsonExported -> {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            event.file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PalateLog Backup"))
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to share backup: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Sync", fontWeight = FontWeight.Bold) },
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
            // Section 1: Family Member Profiles
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, tint = AmberPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Family Members (${uiState.members.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Member", tint = AmberPrimary)
                        }
                    }

                    Text(
                        text = "Used for tagging specific food preferences (e.g. \"Wife loved this, Karthik avoided\").",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    uiState.members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(AmberPrimary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = member.displayName + if (member.isPrimaryUser) " (Primary)" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!member.isPrimaryUser) {
                                IconButton(
                                    onClick = { viewModel.removeFamilyMember(member.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Google Shared Drive Sync
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = AmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Shared Drive Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Synchronizes shared food learnings across your family devices with zero monthly server costs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val syncTimeText = if (uiState.lastSyncedAt > 0) {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(uiState.lastSyncedAt))
                    } else "Never"

                    Text(
                        text = "Last Synced: $syncTimeText",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (uiState.syncMessage.isNotBlank()) {
                        Text(
                            text = uiState.syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerSync() },
                        enabled = !uiState.isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Now")
                        }
                    }
                }
            }

            // Section 3: Backup & Export
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Offline Backup & Transfer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportBackup() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export JSON")
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import JSON")
                        }
                    }
                }
            }

            // Section 4: About PalateLog
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About PalateLog",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Application ID: ${BuildConfig.APPLICATION_ID}\nVersion: ${BuildConfig.VERSION_NAME}\nLicense: MIT License",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Add Family Member Dialog
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Family Member") },
            text = {
                OutlinedTextField(
                    value = newMemberName,
                    onValueChange = { newMemberName = it },
                    label = { Text("Member Name") },
                    placeholder = { Text("e.g. Wife, Son, Daughter") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newMemberName.isNotBlank()) {
                        viewModel.addFamilyMember(newMemberName)
                        newMemberName = ""
                        showAddMemberDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import JSON Backup") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup File 📁")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR PASTE JSON  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Paste JSON Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importBackup(importJsonText)
                            importJsonText = ""
                            showImportDialog = false
                        }
                    },
                    enabled = importJsonText.isNotBlank()
                ) {
                    Text("Restore Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
