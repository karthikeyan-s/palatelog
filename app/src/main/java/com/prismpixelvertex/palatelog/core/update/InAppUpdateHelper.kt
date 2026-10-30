package com.prismpixelvertex.palatelog.core.update

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "InAppUpdateHelper"

class InAppUpdateHelper(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    private val _isUpdateDownloaded = MutableStateFlow(value = false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.i(TAG, "In-app update download completed (InstallStatus.DOWNLOADED)")
                _isUpdateDownloaded.value = true
            }
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                Log.d(TAG, "In-app update downloading: $bytesDownloaded / $totalBytes bytes")
            }
            InstallStatus.FAILED -> {
                Log.w(TAG, "In-app update download failed with error code: ${state.installErrorCode()}")
                isFlowInProgress = false
            }
            InstallStatus.CANCELED -> {
                Log.i(TAG, "In-app update download canceled")
                isFlowInProgress = false
            }
            else -> {
                Log.d(TAG, "In-app update install status: ${state.installStatus()}")
            }
        }
    }

    fun registerListener() {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private var isFlowInProgress = false

    fun resetFlowState() {
        isFlowInProgress = false
    }

    fun checkForAppUpdate(updateLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        if (isFlowInProgress) return

        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        // Default staleness threshold: 90 days (0 or negative means disabled)
        val stalenessThresholdDays = prefs.getInt("staleness_days_threshold", 90)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            // Priority 1: Check if an update is already downloaded and waiting to be installed.
            // When downloaded, updateAvailability() may still report UPDATE_AVAILABLE, so installStatus must be checked first!
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Log.i(TAG, "In-app update is already downloaded and ready to install")
                _isUpdateDownloaded.value = true
                return@addOnSuccessListener
            }

            // Priority 2: Check for a new available update.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val priority = appUpdateInfo.updatePriority()
                val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0

                val isForced = (priority >= 4) || (stalenessThresholdDays in 1..stalenessDays)

                if (isForced && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    isFlowInProgress = true
                    Log.i(TAG, "Starting IMMEDIATE in-app update flow (forced: priority=$priority, staleness=$stalenessDays)")
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    )
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    isFlowInProgress = true
                    Log.i(TAG, "Starting FLEXIBLE in-app update flow")
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                }
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "Failed to check for app update", error)
        }
    }

    /**
     * Safe lifecycle check called on Activity onResume().
     * Only checks if an update finished downloading while backgrounded, or resumes an
     * interrupted IMMEDIATE update. Never initiates a new flexible update flow or prompt loop.
     */
    fun checkStatusOnResume(updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Log.i(TAG, "onResume: In-app update is downloaded, notifying UI")
                _isUpdateDownloaded.value = true
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an immediate update was in progress and interrupted, resume it
                if (updateLauncher != null) {
                    try {
                        Log.i(TAG, "onResume: Resuming in-progress immediate update")
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to resume immediate update in onResume", e)
                    }
                }
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "onResume: Failed to query app update info", error)
        }
    }

    fun completeUpdate() {
        Log.i(TAG, "Completing update and requesting app restart")
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.w(TAG, "completeUpdate: ${e.message}")
        }
    }

    /**
     * Testing hook to simulate the downloaded state directly (via ADB broadcast or unit test).
     */
    fun setUpdateDownloadedForTesting(downloaded: Boolean) {
        Log.i(TAG, "Test hook: setUpdateDownloadedForTesting($downloaded)")
        _isUpdateDownloaded.value = downloaded
    }

    fun getStalenessThresholdDays(): Int {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("staleness_days_threshold", 90)
    }

    fun setStalenessThresholdDays(days: Int) {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        prefs.edit { putInt("staleness_days_threshold", days) }
    }
}
