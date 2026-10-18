package com.karthik.palatelog.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthik.palatelog.core.database.RestaurantDao
import com.karthik.palatelog.core.location.GeoUtils
import com.karthik.palatelog.core.location.LocationClient
import com.karthik.palatelog.core.model.Restaurant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val userLatitude: Double = 12.9716, // Default Bangalore center fallback
    val userLongitude: Double = 77.5946,
    val selectedRestaurant: Restaurant? = null,
    val selectedDistanceKm: Double? = null,
    val isLocationReady: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val restaurantDao: RestaurantDao,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        restaurantDao.getAllRestaurants(),
        _userLocation,
        _selectedRestaurant
    ) { restaurantEntities, userLoc, selected ->
        val domainRestaurants = restaurantEntities.map { it.toDomain() }
        val dist = if (selected != null && userLoc != null) {
            GeoUtils.calculateDistanceKm(userLoc.first, userLoc.second, selected.latitude, selected.longitude)
        } else null

        MapUiState(
            restaurants = domainRestaurants,
            userLatitude = userLoc?.first ?: 12.9716,
            userLongitude = userLoc?.second ?: 77.5946,
            selectedRestaurant = selected,
            selectedDistanceKm = dist,
            isLocationReady = userLoc != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        loadLocation()
    }

    fun loadLocation() {
        viewModelScope.launch {
            val loc = locationClient.getCurrentLocation()
            if (loc != null) {
                _userLocation.value = Pair(loc.latitude, loc.longitude)
            }
        }
    }

    fun onMarkerSelected(restaurant: Restaurant?) {
        _selectedRestaurant.value = restaurant
    }
}
