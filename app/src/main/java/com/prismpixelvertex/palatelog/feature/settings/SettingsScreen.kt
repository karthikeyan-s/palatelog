package com.prismpixelvertex.palatelog.feature.settings

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.prismpixelvertex.palatelog.BuildConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.android.gms.common.AccountPicker
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
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

    var showEditPrimaryDialog by remember { mutableStateOf(false) }
    var primaryNameInput by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmailsInput by remember { mutableStateOf("") }

    var showShareQrDialog by remember { mutableStateOf(false) }
    var showJoinFolderDialog by remember { mutableStateOf(false) }
    var joinFolderIdInput by remember { mutableStateOf("") }

    var isSyncModeExpanded by remember { mutableStateOf(uiState.targetFolderId.isNullOrBlank()) }

    var pendingPermissionDialogEvent by remember { mutableStateOf<SettingsEvent.ShowPermissionPendingDialog?>(null) }

    var pendingConsentEmail by remember { mutableStateOf<String?>(null) }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("PalateLog_Sync", "accountPickerLauncher returned resultCode: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            Log.d("PalateLog_Sync", "Selected Google Account: $email")
            if (!email.isNullOrBlank()) {
                viewModel.onGoogleSignInSuccess(email)
            } else {
                Toast.makeText(context, "No Google Account selected", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(context, "Account selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val authConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("PalateLog_Sync", "authConsentLauncher returned resultCode: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            pendingConsentEmail?.let { email ->
                viewModel.onAuthConsentGranted(email)
            } ?: viewModel.triggerSync()
        } else {
            Toast.makeText(context, "Google Drive access permission was not granted", Toast.LENGTH_SHORT).show()
        }
    }

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
                is SettingsEvent.LaunchAuthConsent -> {
                    pendingConsentEmail = event.pendingEmail
                    authConsentLauncher.launch(event.intent)
                }
                is SettingsEvent.ShowPermissionPendingDialog -> {
                    pendingPermissionDialogEvent = event
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            if (member.isPrimaryUser) {
                                IconButton(
                                    onClick = {
                                        primaryNameInput = member.displayName
                                        showEditPrimaryDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Profile Name",
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
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
                        text = "Synchronizes shared food learnings across your family devices via Google Drive with zero server costs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.isSignedInWithGoogle) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AmberPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Google Drive Connected",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = uiState.googleAccountEmail ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.onGoogleSignOut() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Disconnect",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Collapsible Sync Mode Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isSyncModeExpanded = !isSyncModeExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Family Sync Setup Mode",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.syncMode == "INDIVIDUAL_ACCOUNTS") "(Individual)" else "(Shared)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = if (isSyncModeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isSyncModeExpanded) "Collapse" else "Expand",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isSyncModeExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.setSyncMode("SHARED_ACCOUNT") },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (uiState.syncMode == "SHARED_ACCOUNT") AmberPrimary.copy(alpha = 0.15f) else Color.Transparent
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Shared Account",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (uiState.syncMode == "SHARED_ACCOUNT") AmberPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.setSyncMode("INDIVIDUAL_ACCOUNTS")
                                                viewModel.loadInvitedMembers()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (uiState.syncMode == "INDIVIDUAL_ACCOUNTS") AmberPrimary.copy(alpha = 0.15f) else Color.Transparent
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Individual Accounts",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (uiState.syncMode == "INDIVIDUAL_ACCOUNTS") AmberPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    if (uiState.syncMode == "INDIVIDUAL_ACCOUNTS") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Individual Accounts Management",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            if (!uiState.targetFolderId.isNullOrBlank()) {
                                                Text(
                                                    text = "Family Folder ID: ${uiState.targetFolderId}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showInviteDialog = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("➕ Invite Email", style = MaterialTheme.typography.labelSmall)
                                                }

                                                OutlinedButton(
                                                    onClick = { showShareQrDialog = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("📱 Share Link", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { showJoinFolderDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🔗 Join via Folder Link / ID", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                Log.d("PalateLog_Sync", "Launching Google AccountPicker dialog...")
                                val intent = AccountPicker.newChooseAccountIntent(
                                    AccountPicker.AccountChooserOptions.Builder()
                                        .setAllowableAccountsTypes(listOf("com.google"))
                                        .setTitleOverrideText("Select Google Account for Drive Sync")
                                        .setAlwaysShowAccountPicker(true)
                                        .build()
                                )
                                accountPickerLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Google Drive for Cloud Sync")
                        }
                    }

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

            Spacer(modifier = Modifier.height(32.dp))
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

    // Edit Primary Profile Name Dialog
    if (showEditPrimaryDialog) {
        AlertDialog(
            onDismissRequest = { showEditPrimaryDialog = false },
            title = { Text("Edit Primary Profile Name") },
            text = {
                OutlinedTextField(
                    value = primaryNameInput,
                    onValueChange = { primaryNameInput = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Karthik, Priya, Me") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (primaryNameInput.isNotBlank()) {
                        viewModel.updatePrimaryMemberName(primaryNameInput)
                        showEditPrimaryDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPrimaryDialog = false }) {
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

    // Invite Family Member Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Family Member", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter Google Account email addresses to grant access to your family sync folder:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inviteEmailsInput,
                        onValueChange = { inviteEmailsInput = it },
                        label = { Text("Email Address(es)") },
                        placeholder = { Text("e.g. wife@gmail.com, son@gmail.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteEmailsInput.isNotBlank()) {
                            viewModel.inviteFamilyMember(inviteEmailsInput)
                            inviteEmailsInput = ""
                            showInviteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share Family Sync Link Dialog
    if (showShareQrDialog) {
        val folderId = uiState.targetFolderId ?: "Pending Setup"
        val shareLink = "palatelog://sync?folderId=$folderId"
        AlertDialog(
            onDismissRequest = { showShareQrDialog = false },
            title = { Text("Family Sync Share Link", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Share this link or Folder ID with your family members so they can connect their PalateLog app:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Folder ID:\n$folderId",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareText = "Join our PalateLog Family Sync!\n\nFolder ID: $folderId\nLink: $shareLink"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PalateLog Family Link"))
                        showShareQrDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Share via WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareQrDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Join Family Folder Dialog
    if (showJoinFolderDialog) {
        AlertDialog(
            onDismissRequest = { showJoinFolderDialog = false },
            title = { Text("Join Family Sync Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter or paste the Family Folder ID shared by your host:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = joinFolderIdInput,
                        onValueChange = { joinFolderIdInput = it },
                        label = { Text("Folder ID") },
                        placeholder = { Text("e.g. 1a2b3c4d5e...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinFolderIdInput.isNotBlank()) {
                            viewModel.setTargetFolderId(joinFolderIdInput.trim())
                            viewModel.setSyncMode("INDIVIDUAL_ACCOUNTS")
                            viewModel.triggerSync()
                            showJoinFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Connect Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permission Pending Dialog
    pendingPermissionDialogEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingPermissionDialogEvent = null },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Access Permission Pending",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your account (${event.userEmail}) does not have access to this family sync folder yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Please ask the family host to add ${event.userEmail} under Family Sync Settings in their PalateLog app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val message = "Hi! I'm trying to join our family's PalateLog sync. Could you please add my email (${event.userEmail}) under Family Sync Settings in your PalateLog app?"
                        val waIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                        }
                        try {
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Ask Host"))
                        }
                        pendingPermissionDialogEvent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Ask Host via WhatsApp")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingPermissionDialogEvent = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
