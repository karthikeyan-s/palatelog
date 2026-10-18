package com.karthik.palatelog.feature.restaurant_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthik.palatelog.core.database.DishFeedbackDao
import com.karthik.palatelog.core.database.RestaurantDao
import com.karthik.palatelog.core.database.VisitDao
import com.karthik.palatelog.core.model.DishFeedback
import com.karthik.palatelog.core.model.DishVerdict
import com.karthik.palatelog.core.model.Restaurant
import com.karthik.palatelog.core.model.VisitEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantDetailUiState(
    val restaurant: Restaurant? = null,
    val visits: List<VisitEntry> = emptyList(),
    val mustTryDishes: List<DishFeedback> = emptyList(),
    val avoidDishes: List<DishFeedback> = emptyList(),
    val neutralDishes: List<DishFeedback> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao
) : ViewModel() {

    val restaurantId: String = checkNotNull(savedStateHandle["restaurantId"])

    val uiState: StateFlow<RestaurantDetailUiState> = combine(
        restaurantDao.getRestaurantById(restaurantId),
        visitDao.getVisitsForRestaurant(restaurantId),
        dishDao.getDishesForRestaurant(restaurantId)
    ) { restaurantEntity, visitEntities, dishEntities ->
        val restaurant = restaurantEntity?.toDomain()
        val visits = visitEntities.map { it.toDomain() }
        val dishes = dishEntities.map { it.toDomain() }

        val mustTry = dishes.filter { it.verdict == DishVerdict.GO }.distinctBy { it.dishName.lowercase() }
        val avoid = dishes.filter { it.verdict == DishVerdict.NO_GO }.distinctBy { it.dishName.lowercase() }
        val neutral = dishes.filter { it.verdict == DishVerdict.MEH }.distinctBy { it.dishName.lowercase() }

        RestaurantDetailUiState(
            restaurant = restaurant,
            visits = visits,
            mustTryDishes = mustTry,
            avoidDishes = avoid,
            neutralDishes = neutral,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RestaurantDetailUiState(isLoading = true)
    )

    fun deleteRestaurant() {
        viewModelScope.launch {
            restaurantDao.softDelete(restaurantId)
        }
    }
}
