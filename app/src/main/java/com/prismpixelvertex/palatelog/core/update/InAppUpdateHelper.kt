package com.prismpixelvertex.palatelog.core.update

import android.app.Activity
import android.content.Context
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

class InAppUpdateHelper(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _isUpdateDownloaded.value = true
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

    fun checkForAppUpdate(
        activity: Activity,
        requestCodeFlexible: Int = 1001,
        requestCodeImmediate: Int = 1002
    ) {
        if (isFlowInProgress) return

        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        // Default staleness threshold: 90 days (0 or negative means disabled)
        val stalenessThresholdDays = prefs.getInt("staleness_days_threshold", 90)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val priority = appUpdateInfo.updatePriority()
                val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0

                val isForced = (priority >= 4) || (stalenessThresholdDays in 1..stalenessDays)

                if (isForced && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    isFlowInProgress = true
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        requestCodeImmediate
                    )
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    isFlowInProgress = true
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        requestCodeFlexible
                    )
                }
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _isUpdateDownloaded.value = true
            }
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun getStalenessThresholdDays(): Int {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("staleness_days_threshold", 90)
    }

    fun setStalenessThresholdDays(days: Int) {
        val prefs = context.getSharedPreferences("palatelog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("staleness_days_threshold", days).apply()
    }
}
