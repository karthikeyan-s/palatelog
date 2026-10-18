package com.karthik.palatelog.feature.quick_capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthik.palatelog.core.database.DishFeedbackDao
import com.karthik.palatelog.core.database.DishFeedbackEntity
import com.karthik.palatelog.core.database.RestaurantDao
import com.karthik.palatelog.core.database.RestaurantEntity
import com.karthik.palatelog.core.database.VisitDao
import com.karthik.palatelog.core.database.VisitEntryEntity
import com.karthik.palatelog.core.location.GeoUtils
import com.karthik.palatelog.core.location.LocationClient
import com.karthik.palatelog.core.model.Channel
import com.karthik.palatelog.core.model.DishCategory
import com.karthik.palatelog.core.model.DishVerdict
import com.karthik.palatelog.core.model.RestaurantType
import com.karthik.palatelog.core.model.Verdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuickCaptureUiState(
    val placeName: String = "",
    val streetOrArea: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isLocationLoading: Boolean = true,
    val selectedVerdict: Verdict = Verdict.NO_GO, // Default to NO-GO for speed-dial regret
    val quickReason: String = "",
    val dishName: String = "",
    val isHighwayStop: Boolean = false,
    val highwayName: String = "",
    val nearbyExistingRestaurant: RestaurantEntity? = null,
    val isSaving: Boolean = false
)

sealed interface QuickCaptureEvent {
    object SavedSuccessfully : QuickCaptureEvent
    data class Error(val message: String) : QuickCaptureEvent
}

@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    private val locationClient: LocationClient,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuickCaptureEvent>()
    val events: SharedFlow<QuickCaptureEvent> = _events.asSharedFlow()

    init {
        acquireLocation()
    }

    fun acquireLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true)
            val loc = locationClient.getCurrentLocation()
            if (loc != null) {
                // Check if an existing restaurant is within 50 meters
                val bbox = GeoUtils.getBoundingBox(loc.latitude, loc.longitude, 0.1) // 100m
                val nearby = restaurantDao.getRestaurantsInBoundingBox(bbox.minLat, bbox.maxLat, bbox.minLng, bbox.maxLng)
                val closest = nearby.minByOrNull {
                    GeoUtils.calculateDistanceKm(loc.latitude, loc.longitude, it.latitude, it.longitude)
                }

                _uiState.value = _uiState.value.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    placeName = if (_uiState.value.placeName.isBlank()) loc.placeName else _uiState.value.placeName,
                    streetOrArea = loc.street,
                    city = loc.city,
                    nearbyExistingRestaurant = closest,
                    isLocationLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLocationLoading = false)
            }
        }
    }

    fun onPlaceNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(placeName = name)
    }

    fun onVerdictSelected(verdict: Verdict) {
        _uiState.value = _uiState.value.copy(selectedVerdict = verdict)
    }

    fun onQuickReasonChanged(reason: String) {
        _uiState.value = _uiState.value.copy(quickReason = reason)
    }

    fun onDishNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(dishName = name)
    }

    fun onHighwayToggle(isHighway: Boolean) {
        _uiState.value = _uiState.value.copy(isHighwayStop = isHighway)
    }

    fun onHighwayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(highwayName = name)
    }

    fun onUseNearbyRestaurant(restaurant: RestaurantEntity) {
        _uiState.value = _uiState.value.copy(
            placeName = restaurant.name,
            selectedVerdict = try { Verdict.valueOf(restaurant.overallVerdict) } catch (e: Exception) { Verdict.NO_GO },
            nearbyExistingRestaurant = null
        )
    }

    fun saveQuickLog() {
        val state = _uiState.value
        if (state.placeName.isBlank()) {
            viewModelScope.launch {
                _events.emit(QuickCaptureEvent.Error("Please enter the restaurant or stall name"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val now = System.currentTimeMillis()
                val restaurantId = UUID.randomUUID().toString()

                val restaurantType = if (state.isHighwayStop) RestaurantType.DHABA else RestaurantType.DINE_IN

                val restaurantEntity = RestaurantEntity(
                    id = restaurantId,
                    name = state.placeName.trim(),
                    latitude = state.latitude,
                    longitude = state.longitude,
                    address = state.streetOrArea,
                    city = state.city,
                    highwayOrRoute = if (state.isHighwayStop) state.highwayName.ifBlank { "Highway Route" } else "",
                    cuisine = "",
                    restaurantType = restaurantType.name,
                    overallVerdict = state.selectedVerdict.name,
                    tags = if (state.quickReason.isNotBlank()) listOf(state.quickReason) else emptyList(),
                    isShared = true,
                    createdAt = now,
                    updatedAt = now,
                    isDeleted = false
                )
                restaurantDao.insert(restaurantEntity)

                val visitId = UUID.randomUUID().toString()
                val visitChannel = if (state.isHighwayStop) Channel.HIGHWAY_STOP else Channel.WALK_IN
                val visitEntity = VisitEntryEntity(
                    id = visitId,
                    restaurantId = restaurantId,
                    visitDate = now,
                    channel = visitChannel.name,
                    visitVerdict = state.selectedVerdict.name,
                    billAmount = 0.0,
                    billReceiptPhotoUri = null,
                    generalNotes = state.quickReason,
                    createdByMemberId = "",
                    createdByMemberName = "Me",
                    isShared = true,
                    updatedAt = now,
                    isDeleted = false
                )
                visitDao.insert(visitEntity)

                if (state.dishName.isNotBlank()) {
                    val dishVerdict = when (state.selectedVerdict) {
                        Verdict.GO -> DishVerdict.GO
                        Verdict.NO_GO -> DishVerdict.NO_GO
                        Verdict.CAUTION -> DishVerdict.MEH
                    }
                    val dishEntity = DishFeedbackEntity(
                        id = UUID.randomUUID().toString(),
                        visitId = visitId,
                        restaurantId = restaurantId,
                        dishName = state.dishName.trim(),
                        category = DishCategory.MAIN.name,
                        verdict = dishVerdict.name,
                        tags = if (state.quickReason.isNotBlank()) listOf(state.quickReason) else emptyList(),
                        memberPreferences = emptyMap(),
                        price = 0.0,
                        photoUris = emptyList(),
                        notes = state.quickReason,
                        isShared = true,
                        updatedAt = now,
                        isDeleted = false
                    )
                    dishDao.insert(dishEntity)
                }

                _events.emit(QuickCaptureEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.emit(QuickCaptureEvent.Error(e.localizedMessage ?: "Failed to save"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
