package com.prismpixelvertex.palatelog.core.designsystem

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import com.prismpixelvertex.palatelog.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported map rendering providers for plug-and-play architecture.
 */
enum class MapProviderType(
    @get:StringRes val nameResId: Int,
    @get:StringRes val attributionResId: Int,
    val isGovernmentCompliant: Boolean,
) {
    MAPPLS(
        nameResId = R.string.map_provider_mappls_name,
        attributionResId = R.string.map_provider_mappls_attr,
        isGovernmentCompliant = true,
    ),
    OSM_DROID(
        nameResId = R.string.map_provider_osm_name,
        attributionResId = R.string.map_provider_osm_attr,
        isGovernmentCompliant = false,
    ),
    GOOGLE_MAPS(
        nameResId = R.string.map_provider_google_name,
        attributionResId = R.string.map_provider_google_attr,
        isGovernmentCompliant = true,
    ),
    OLA_MAPS(
        nameResId = R.string.map_provider_ola_name,
        attributionResId = R.string.map_provider_ola_attr,
        isGovernmentCompliant = true,
    );
}

/**
 * Snapshot of the currently configured and effective map provider state.
 */
data class ActiveMapProviderState(
    val primaryProvider: MapProviderType,
    val effectiveProvider: MapProviderType,
    val isFallbackEngaged: Boolean,
)

/**
 * Runtime configuration and fallback state for map providers.
 */
object MapProviderConfig {
    @Volatile
    var isMapplsInitialized: Boolean = false
        private set

    @Volatile
    var activeProvider: MapProviderType = MapProviderType.MAPPLS

    private val _providerState = MutableStateFlow(resolveState())
    val providerState: StateFlow<ActiveMapProviderState> = _providerState.asStateFlow()

    /**
     * Resolves the active provider, falling back to OSM_DROID if Mappls is unavailable or uninitialized.
     */
    fun getEffectiveProvider(): MapProviderType {
        return resolveState().effectiveProvider
    }

    fun notifyMapplsInitialized(success: Boolean) {
        isMapplsInitialized = success
        _providerState.value = resolveState()
    }

    fun notifyFallbackEngaged() {
        isMapplsInitialized = false
        _providerState.value = resolveState()
    }

    private fun resolveState(): ActiveMapProviderState {
        val effective = if ((activeProvider == MapProviderType.MAPPLS) && isMapplsInitialized) {
            MapProviderType.MAPPLS
        } else if ((activeProvider == MapProviderType.MAPPLS) && !isMapplsInitialized) {
            MapProviderType.OSM_DROID
        } else {
            activeProvider
        }
        val isFallback = (activeProvider != effective)
        return ActiveMapProviderState(
            primaryProvider = activeProvider,
            effectiveProvider = effective,
            isFallbackEngaged = isFallback
        )
    }
}

/**
 * Platform-agnostic marker data contract for PalateLog maps.
 */
data class PalateMapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String = "",
    val customIcon: Drawable? = null
)
