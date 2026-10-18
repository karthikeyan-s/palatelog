package com.karthik.palatelog.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthik.palatelog.core.database.FamilyMemberDao
import com.karthik.palatelog.core.database.FamilyMemberEntity
import com.karthik.palatelog.core.model.FamilyMember
import com.karthik.palatelog.core.sync.SyncRepository
import com.karthik.palatelog.core.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val members: List<FamilyMember> = emptyList(),
    val lastSyncedAt: Long = 0L,
    val isSyncing: Boolean = false,
    val syncMessage: String = ""
)

sealed interface SettingsEvent {
    data class JsonExported(val file: File) : SettingsEvent
    data class ShowToast(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val memberDao: FamilyMemberDao,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        memberDao.getAllMembers(),
        syncRepository.syncStatus
    ) { memberEntities, syncStatus ->
        val members = memberEntities.map { it.toDomain() }
        val isSyncing = syncStatus is SyncStatus.Syncing
        val message = when (syncStatus) {
            is SyncStatus.Success -> syncStatus.message
            is SyncStatus.Error -> "Error: ${syncStatus.error}"
            else -> ""
        }

        SettingsUiState(
            members = members,
            lastSyncedAt = syncRepository.getLastSyncTimestamp(),
            isSyncing = isSyncing,
            syncMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
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
                        displayName = "Karthik",
                        avatarColorHex = "#F59E0B",
                        isPrimaryUser = true
                    )
                )
                memberDao.insert(
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        displayName = "Wife",
                        avatarColorHex = "#10B981",
                        isPrimaryUser = false
                    )
                )
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

    fun triggerSync() {
        viewModelScope.launch {
            try {
                val payload = syncRepository.exportSharedPayload(syncRepository.getLastSyncTimestamp())
                syncRepository.importPayload(payload)
                _events.emit(SettingsEvent.ShowToast("Synced successfully!"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Sync failed: ${e.localizedMessage}"))
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
}
