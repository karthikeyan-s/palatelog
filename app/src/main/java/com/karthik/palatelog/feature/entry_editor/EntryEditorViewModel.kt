package com.karthik.palatelog.feature.entry_editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthik.palatelog.core.database.DishFeedbackDao
import com.karthik.palatelog.core.database.DishFeedbackEntity
import com.karthik.palatelog.core.database.FamilyMemberDao
import com.karthik.palatelog.core.database.RestaurantDao
import com.karthik.palatelog.core.database.RestaurantEntity
import com.karthik.palatelog.core.database.VisitDao
import com.karthik.palatelog.core.database.VisitEntryEntity
import com.karthik.palatelog.core.location.LocationClient
import com.karthik.palatelog.core.model.Channel
import com.karthik.palatelog.core.model.DishCategory
import com.karthik.palatelog.core.model.DishFeedback
import com.karthik.palatelog.core.model.DishVerdict
import com.karthik.palatelog.core.model.FamilyMember
import com.karthik.palatelog.core.model.RestaurantType
import com.karthik.palatelog.core.model.Verdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DishDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: DishCategory = DishCategory.MAIN,
    val verdict: DishVerdict = DishVerdict.GO,
    val tagsText: String = "",
    val priceText: String = "",
    val memberReaction: String = "",
    val selectedMemberName: String = ""
)

data class EntryEditorUiState(
    val restaurantId: String? = null,
    val restaurantName: String = "",
    val existingRestaurants: List<RestaurantEntity> = emptyList(),
    val previousDishes: List<DishFeedbackEntity> = emptyList(),
    val channel: Channel = Channel.WALK_IN,
    val overallVerdict: Verdict = Verdict.GO,
    val cuisine: String = "",
    val address: String = "",
    val city: String = "",
    val highwayOrRoute: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val billAmountText: String = "",
    val billPhotoUri: String? = null,
    val generalNotes: String = "",
    val dishes: List<DishDraft> = emptyList(),
    val familyMembers: List<FamilyMember> = emptyList(),
    val isShared: Boolean = true,
    val isSaving: Boolean = false
)

sealed interface EntryEditorEvent {
    object SavedSuccessfully : EntryEditorEvent
    data class Error(val message: String) : EntryEditorEvent
}

@HiltViewModel
class EntryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao,
    private val memberDao: FamilyMemberDao,
    private val locationClient: LocationClient
) : ViewModel() {

    private val initialRestaurantId: String? = savedStateHandle["restaurantId"]

    private val _uiState = MutableStateFlow(EntryEditorUiState(restaurantId = initialRestaurantId))
    val uiState: StateFlow<EntryEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EntryEditorEvent>()
    val events: SharedFlow<EntryEditorEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load existing restaurants for suggestions
            val allRestaurants = restaurantDao.getAllRestaurants().firstOrNull() ?: emptyList()
            // Load family members
            val members = memberDao.getAllMembers().firstOrNull()?.map { it.toDomain() } ?: emptyList()

            _uiState.value = _uiState.value.copy(
                existingRestaurants = allRestaurants,
                familyMembers = members
            )

            // If a restaurantId was passed, load its details
            if (!initialRestaurantId.isNullOrBlank()) {
                val existing = restaurantDao.getRestaurantByIdSync(initialRestaurantId)
                if (existing != null) {
                    selectExistingRestaurant(existing)
                }
            } else {
                // Fetch GPS location to pre-fill coordinates
                val loc = locationClient.getCurrentLocation()
                if (loc != null) {
                    _uiState.value = _uiState.value.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = loc.street,
                        city = loc.city
                    )
                }
            }
        }
    }

    fun onRestaurantNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(restaurantName = name)
        // Check if matches an existing restaurant
        val match = _uiState.value.existingRestaurants.firstOrNull {
            it.name.equals(name.trim(), ignoreCase = true)
        }
        if (match != null) {
            selectExistingRestaurant(match)
        }
    }

    fun selectExistingRestaurant(restaurant: RestaurantEntity) {
        viewModelScope.launch {
            val previousDishes = dishDao.getDishesForRestaurant(restaurant.id).firstOrNull() ?: emptyList()
            _uiState.value = _uiState.value.copy(
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                cuisine = restaurant.cuisine,
                address = restaurant.address,
                city = restaurant.city,
                highwayOrRoute = restaurant.highwayOrRoute,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                overallVerdict = try { Verdict.valueOf(restaurant.overallVerdict) } catch (e: Exception) { Verdict.GO },
                previousDishes = previousDishes
            )
        }
    }

    fun onChannelSelected(channel: Channel) {
        _uiState.value = _uiState.value.copy(channel = channel)
    }

    fun onVerdictSelected(verdict: Verdict) {
        _uiState.value = _uiState.value.copy(overallVerdict = verdict)
    }

    fun onCuisineChanged(cuisine: String) {
        _uiState.value = _uiState.value.copy(cuisine = cuisine)
    }

    fun onHighwayOrRouteChanged(route: String) {
        _uiState.value = _uiState.value.copy(highwayOrRoute = route)
    }

    fun onBillAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(billAmountText = amount)
    }

    fun onGeneralNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(generalNotes = notes)
    }

    fun onSharedToggle(isShared: Boolean) {
        _uiState.value = _uiState.value.copy(isShared = isShared)
    }

    // Dish Management
    fun addDishDraft(dishName: String = "", verdict: DishVerdict = DishVerdict.GO, tags: String = "") {
        val currentDishes = _uiState.value.dishes.toMutableList()
        currentDishes.add(
            DishDraft(
                name = dishName,
                verdict = verdict,
                tagsText = tags
            )
        )
        _uiState.value = _uiState.value.copy(dishes = currentDishes)
    }

    fun updateDishDraft(index: Int, updated: DishDraft) {
        val current = _uiState.value.dishes.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _uiState.value = _uiState.value.copy(dishes = current)
        }
    }

    fun removeDishDraft(index: Int) {
        val current = _uiState.value.dishes.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(dishes = current)
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.restaurantName.isBlank()) {
            viewModelScope.launch {
                _events.emit(EntryEditorEvent.Error("Restaurant name cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val now = System.currentTimeMillis()
                val restaurantId = state.restaurantId ?: UUID.randomUUID().toString()

                val restaurantType = when (state.channel) {
                    Channel.HIGHWAY_STOP -> RestaurantType.DHABA
                    Channel.ZOMATO, Channel.SWIGGY -> RestaurantType.CLOUD_KITCHEN
                    Channel.DRIVE_THRU -> RestaurantType.DRIVE_THRU
                    else -> RestaurantType.DINE_IN
                }

                // 1. Insert or Update Restaurant
                val restaurantEntity = RestaurantEntity(
                    id = restaurantId,
                    name = state.restaurantName.trim(),
                    latitude = state.latitude,
                    longitude = state.longitude,
                    address = state.address,
                    city = state.city,
                    highwayOrRoute = state.highwayOrRoute,
                    cuisine = state.cuisine,
                    restaurantType = restaurantType.name,
                    overallVerdict = state.overallVerdict.name,
                    tags = emptyList(),
                    isShared = state.isShared,
                    createdAt = now,
                    updatedAt = now,
                    isDeleted = false
                )
                restaurantDao.insert(restaurantEntity)

                // 2. Insert Visit Entry
                val visitId = UUID.randomUUID().toString()
                val billAmount = state.billAmountText.toDoubleOrNull() ?: 0.0

                val visitEntity = VisitEntryEntity(
                    id = visitId,
                    restaurantId = restaurantId,
                    visitDate = now,
                    channel = state.channel.name,
                    visitVerdict = state.overallVerdict.name,
                    billAmount = billAmount,
                    billReceiptPhotoUri = state.billPhotoUri,
                    generalNotes = state.generalNotes,
                    createdByMemberId = "",
                    createdByMemberName = "Me",
                    isShared = state.isShared,
                    updatedAt = now,
                    isDeleted = false
                )
                visitDao.insert(visitEntity)

                // 3. Insert Dishes
                val dishEntities = state.dishes.filter { it.name.isNotBlank() }.map { draft ->
                    val memberPrefMap = if (draft.selectedMemberName.isNotBlank() && draft.memberReaction.isNotBlank()) {
                        mapOf(draft.selectedMemberName to draft.memberReaction)
                    } else emptyMap()

                    DishFeedbackEntity(
                        id = draft.id,
                        visitId = visitId,
                        restaurantId = restaurantId,
                        dishName = draft.name.trim(),
                        category = draft.category.name,
                        verdict = draft.verdict.name,
                        tags = if (draft.tagsText.isNotBlank()) draft.tagsText.split(",").map { it.trim() } else emptyList(),
                        memberPreferences = memberPrefMap,
                        price = draft.priceText.toDoubleOrNull() ?: 0.0,
                        photoUris = emptyList(),
                        notes = draft.tagsText,
                        isShared = state.isShared,
                        updatedAt = now,
                        isDeleted = false
                    )
                }
                if (dishEntities.isNotEmpty()) {
                    dishDao.insertAll(dishEntities)
                }

                _events.emit(EntryEditorEvent.SavedSuccessfully)
            } catch (e: Exception) {
                _events.emit(EntryEditorEvent.Error(e.localizedMessage ?: "Failed to save entry"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
