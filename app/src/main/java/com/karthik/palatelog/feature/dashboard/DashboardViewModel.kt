package com.karthik.palatelog.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthik.palatelog.core.database.DishFeedbackDao
import com.karthik.palatelog.core.database.RestaurantDao
import com.karthik.palatelog.core.database.VisitDao
import com.karthik.palatelog.core.location.GeoUtils
import com.karthik.palatelog.core.location.LocationClient
import com.karthik.palatelog.core.model.Channel
import com.karthik.palatelog.core.model.DishFeedback
import com.karthik.palatelog.core.model.Restaurant
import com.karthik.palatelog.core.model.Verdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DashboardFilter {
    ALL,
    GO_ONLY,
    NO_GO_ONLY,
    NEARBY,
    HIGHWAY,
    DELIVERY
}

data class RestaurantCardUiState(
    val restaurant: Restaurant,
    val distanceKm: Double?,
    val totalVisits: Int,
    val topMustTryDishes: List<String>,
    val topAvoidDishes: List<String>
)

data class DashboardUiState(
    val searchQuery: String = "",
    val selectedFilter: DashboardFilter = DashboardFilter.ALL,
    val restaurants: List<RestaurantCardUiState> = emptyList(),
    val totalCount: Int = 0,
    val goCount: Int = 0,
    val noGoCount: Int = 0,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(DashboardFilter.ALL)
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        restaurantDao.getAllRestaurants(),
        visitDao.getAllVisits(),
        _searchQuery,
        _selectedFilter,
        _userLocation
    ) { restaurantEntities, visitEntities, query, filter, location ->
        val domainRestaurants = restaurantEntities.map { it.toDomain() }

        // Filter by Search Query
        val searchFiltered = if (query.isBlank()) {
            domainRestaurants
        } else {
            domainRestaurants.filter { r ->
                r.name.contains(query, ignoreCase = true) ||
                r.city.contains(query, ignoreCase = true) ||
                r.cuisine.contains(query, ignoreCase = true) ||
                r.highwayOrRoute.contains(query, ignoreCase = true) ||
                r.tags.any { it.contains(query, ignoreCase = true) }
            }
        }

        // Filter by Category Chip
        val categoryFiltered = when (filter) {
            DashboardFilter.ALL -> searchFiltered
            DashboardFilter.GO_ONLY -> searchFiltered.filter { it.overallVerdict == Verdict.GO }
            DashboardFilter.NO_GO_ONLY -> searchFiltered.filter { it.overallVerdict == Verdict.NO_GO }
            DashboardFilter.NEARBY -> searchFiltered.filter { r ->
                if (location != null) {
                    val dist = GeoUtils.calculateDistanceKm(location.first, location.second, r.latitude, r.longitude)
                    dist <= 15.0 // within 15 km
                } else true
            }
            DashboardFilter.HIGHWAY -> searchFiltered.filter {
                it.highwayOrRoute.isNotBlank() || it.restaurantType.name == "DHABA"
            }
            DashboardFilter.DELIVERY -> {
                val deliveryKeywords = setOf("zomato", "swiggy", "ownly", "eatclub", "magicpin", "ubereats", "door", "dunzo", "delivery", "cloud")
                val deliveryRestaurantIds = visitEntities
                    .filter { visit ->
                        val lower = visit.channel.lowercase()
                        deliveryKeywords.any { lower.contains(it) }
                    }
                    .map { it.restaurantId }
                    .toSet()
                searchFiltered.filter { it.id in deliveryRestaurantIds || it.restaurantType.name == "CLOUD_KITCHEN" }
            }
        }

        // Map to UI Cards with distances and visit counts
        val cards = categoryFiltered.map { r ->
            val dist = location?.let {
                GeoUtils.calculateDistanceKm(it.first, it.second, r.latitude, r.longitude)
            }
            val visits = visitEntities.count { it.restaurantId == r.id && !it.isDeleted }

            RestaurantCardUiState(
                restaurant = r,
                distanceKm = dist,
                totalVisits = visits,
                topMustTryDishes = emptyList(), // Loaded asynchronously if needed
                topAvoidDishes = emptyList()
            )
        }.sortedWith(
            compareBy<RestaurantCardUiState> { it.distanceKm ?: Double.MAX_VALUE }
                .thenByDescending { it.restaurant.updatedAt }
        )

        DashboardUiState(
            searchQuery = query,
            selectedFilter = filter,
            restaurants = cards,
            totalCount = domainRestaurants.size,
            goCount = domainRestaurants.count { it.overallVerdict == Verdict.GO },
            noGoCount = domainRestaurants.count { it.overallVerdict == Verdict.NO_GO },
            userLatitude = location?.first,
            userLongitude = location?.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    init {
        refreshLocation()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: DashboardFilter) {
        _selectedFilter.value = filter
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = locationClient.getCurrentLocation()
            if (loc != null) {
                _userLocation.value = Pair(loc.latitude, loc.longitude)
            }
        }
    }
}
