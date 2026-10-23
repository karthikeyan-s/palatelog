package com.prismpixelvertex.palatelog.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prismpixelvertex.palatelog.core.database.FamilyMemberDao
import com.prismpixelvertex.palatelog.core.database.FamilyMemberEntity
import com.prismpixelvertex.palatelog.core.model.FamilyMember
import com.prismpixelvertex.palatelog.core.sync.GoogleAuthException
import com.prismpixelvertex.palatelog.core.sync.SyncRepository
import com.prismpixelvertex.palatelog.core.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val members: List<FamilyMember> = emptyList(),
    val googleAccountEmail: String? = null,
    val isSignedInWithGoogle: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val isSyncing: Boolean = false,
    val syncMessage: String = "",
    val stalenessThresholdDays: Int = 90
)

sealed interface SettingsEvent {
    data class JsonExported(val file: File) : SettingsEvent
    data class ShowToast(val message: String) : SettingsEvent
    data class LaunchAuthConsent(val intent: Intent, val pendingEmail: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memberDao: FamilyMemberDao,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val _googleAccountEmail = MutableStateFlow<String?>(syncRepository.getGoogleAccountEmail())
    private val _stalenessThreshold = MutableStateFlow(getStalenessDays())

    private fun getStalenessDays(): Int {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("staleness_days_threshold", 90)
    }

    fun setStalenessThreshold(days: Int) {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("staleness_days_threshold", days).apply()
        _stalenessThreshold.value = days
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        memberDao.getAllMembers(),
        syncRepository.syncStatus,
        _googleAccountEmail,
        _stalenessThreshold
    ) { memberEntities, syncStatus, googleEmail, stalenessDays ->
        val members = memberEntities.map { it.toDomain() }
        val isSyncing = syncStatus is SyncStatus.Syncing
        val message = when (syncStatus) {
            is SyncStatus.Success -> syncStatus.message
            is SyncStatus.Error -> "Error: ${syncStatus.error}"
            else -> ""
        }

        SettingsUiState(
            members = members,
            googleAccountEmail = googleEmail,
            isSignedInWithGoogle = !googleEmail.isNullOrBlank(),
            lastSyncedAt = syncRepository.getLastSyncTimestamp(),
            isSyncing = isSyncing,
            syncMessage = message,
            stalenessThresholdDays = stalenessDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            googleAccountEmail = syncRepository.getGoogleAccountEmail(),
            isSignedInWithGoogle = !syncRepository.getGoogleAccountEmail().isNullOrBlank(),
            stalenessThresholdDays = getStalenessDays()
        )
    )

    init {
        ensureDefaultMember()
    }

    private fun ensureDefaultMember() {
        viewModelScope.launch {
            val primary = memberDao.getPrimaryMember()
            if (primary == null) {
                memberDao.insert(
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        displayName = "Me",
                        avatarColorHex = "#F59E0B",
                        isPrimaryUser = true
                    )
                )
            }
        }
    }

    fun updatePrimaryMemberName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val primary = memberDao.getPrimaryMember()
            if (primary != null) {
                memberDao.update(primary.copy(displayName = trimmed))
            }
        }
    }

    fun addFamilyMember(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val colors = listOf("#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899")
            val chosenColor = colors.random()
            memberDao.insert(
                FamilyMemberEntity(
                    id = UUID.randomUUID().toString(),
                    displayName = name.trim(),
                    avatarColorHex = chosenColor,
                    isPrimaryUser = false
                )
            )
        }
    }

    fun removeFamilyMember(id: String) {
        viewModelScope.launch {
            memberDao.delete(id)
        }
    }

    fun onGoogleSignInSuccess(email: String) {
        viewModelScope.launch {
            connectAndSyncGoogleDrive(email, forceFullExport = true)
        }
    }

    fun onAuthConsentGranted(pendingEmail: String) {
        viewModelScope.launch {
            connectAndSyncGoogleDrive(pendingEmail, forceFullExport = true)
        }
    }

    private suspend fun connectAndSyncGoogleDrive(email: String, forceFullExport: Boolean) {
        try {
            // Temporarily configure email in repository for token request
            syncRepository.setGoogleAccountEmail(email)
            syncRepository.performGoogleDriveSync(forceFullExport = forceFullExport)
            // Auth & Sync succeeded! Permanently save state
            _googleAccountEmail.value = email
            _events.emit(SettingsEvent.ShowToast("Connected & Synced with Google Drive!"))
        } catch (e: GoogleAuthException) {
            // Clear account so state remains disconnected
            syncRepository.setGoogleAccountEmail(null)
            _googleAccountEmail.value = null
            if (e.userRecoverableIntent != null) {
                _events.emit(SettingsEvent.LaunchAuthConsent(e.userRecoverableIntent, email))
            } else {
                _events.emit(SettingsEvent.ShowToast("Google Auth error: ${e.message}"))
            }
        } catch (e: Exception) {
            syncRepository.setGoogleAccountEmail(null)
            _googleAccountEmail.value = null
            _events.emit(SettingsEvent.ShowToast("Drive Sync failed: ${e.localizedMessage}"))
        }
    }

    fun onGoogleSignOut() {
        syncRepository.setGoogleAccountEmail(null)
        _googleAccountEmail.value = null
        viewModelScope.launch {
            _events.emit(SettingsEvent.ShowToast("Signed out of Google Drive"))
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            val email = syncRepository.getGoogleAccountEmail()
            if (!email.isNullOrBlank()) {
                try {
                    syncRepository.performGoogleDriveSync(forceFullExport = false)
                    _events.emit(SettingsEvent.ShowToast("Google Drive Sync completed!"))
                } catch (e: GoogleAuthException) {
                    if (e.userRecoverableIntent != null) {
                        _events.emit(SettingsEvent.LaunchAuthConsent(e.userRecoverableIntent, email))
                    } else {
                        _events.emit(SettingsEvent.ShowToast("Auth error: ${e.message}"))
                    }
                } catch (e: Exception) {
                    _events.emit(SettingsEvent.ShowToast("Sync error: ${e.localizedMessage}"))
                }
            } else {
                try {
                    val payload = syncRepository.exportSharedPayload(syncRepository.getLastSyncTimestamp())
                    syncRepository.importPayload(payload)
                    _events.emit(SettingsEvent.ShowToast("Local sync complete. Connect Google Drive for cloud sync!"))
                } catch (e: Exception) {
                    _events.emit(SettingsEvent.ShowToast("Sync failed: ${e.localizedMessage}"))
                }
            }
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val file = syncRepository.exportToJsonFile()
                _events.emit(SettingsEvent.JsonExported(file))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Export failed: ${e.localizedMessage}"))
            }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                syncRepository.importFromJsonString(jsonString)
                _events.emit(SettingsEvent.ShowToast("Data restored successfully!"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Import failed: ${e.localizedMessage}"))
            }
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: throw IllegalStateException("Could not open file stream")
                }
                syncRepository.importFromJsonString(jsonString)
                _events.emit(SettingsEvent.ShowToast("Data restored successfully!"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Import failed: ${e.localizedMessage}"))
            }
        }
    }
}
