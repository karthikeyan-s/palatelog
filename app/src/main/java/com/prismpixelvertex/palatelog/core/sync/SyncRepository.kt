package com.prismpixelvertex.palatelog.core.sync

import android.content.Context
import android.provider.Settings
import com.prismpixelvertex.palatelog.core.database.DishFeedbackDao
import com.prismpixelvertex.palatelog.core.database.DishFeedbackEntity
import com.prismpixelvertex.palatelog.core.database.RestaurantDao
import com.prismpixelvertex.palatelog.core.database.RestaurantEntity
import com.prismpixelvertex.palatelog.core.database.VisitDao
import com.prismpixelvertex.palatelog.core.database.VisitEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val message: String, val timestamp: Long) : SyncStatus
    data class Error(val error: String) : SyncStatus
}

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val restaurantDao: RestaurantDao,
    private val visitDao: VisitDao,
    private val dishDao: DishFeedbackDao
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val prefs = context.getSharedPreferences("palatelog_sync_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val deviceId: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_unknown"
        } catch (e: Exception) {
            "device_unknown"
        }
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_synced_at", 0L)
    }

    private fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_synced_at", timestamp).apply()
    }

    /**
     * Exports all local shared records modified since the given timestamp.
     */
    suspend fun exportSharedPayload(sinceTimestamp: Long = 0L): SyncPayload = withContext(Dispatchers.IO) {
        val modifiedRestaurants = restaurantDao.getModifiedSince(sinceTimestamp).filter { it.isShared }
        val modifiedVisits = visitDao.getModifiedSince(sinceTimestamp).filter { it.isShared }
        val modifiedDishes = dishDao.getModifiedSince(sinceTimestamp).filter { it.isShared }

        SyncPayload(
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            restaurants = modifiedRestaurants.map {
                SyncRestaurant(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    address = it.address,
                    city = it.city,
                    highwayOrRoute = it.highwayOrRoute,
                    cuisine = it.cuisine,
                    restaurantType = it.restaurantType,
                    overallVerdict = it.overallVerdict,
                    tags = it.tags,
                    isShared = it.isShared,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            visits = modifiedVisits.map {
                SyncVisit(
                    id = it.id,
                    restaurantId = it.restaurantId,
                    visitDate = it.visitDate,
                    channel = it.channel,
                    visitVerdict = it.visitVerdict,
                    billAmount = it.billAmount,
                    generalNotes = it.generalNotes,
                    createdByMemberId = it.createdByMemberId,
                    createdByMemberName = it.createdByMemberName,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            },
            dishes = modifiedDishes.map {
                SyncDish(
                    id = it.id,
                    visitId = it.visitId,
                    restaurantId = it.restaurantId,
                    dishName = it.dishName,
                    category = it.category,
                    verdict = it.verdict,
                    tags = it.tags,
                    memberPreferences = it.memberPreferences,
                    price = it.price,
                    notes = it.notes,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
        )
    }

    /**
     * Ingests a remote SyncPayload into the local database using Last-Write-Wins (LWW).
     */
    suspend fun importPayload(payload: SyncPayload): Unit = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Syncing
        try {
            // 1. Merge Restaurants
            val restaurantEntities = payload.restaurants.map {
                RestaurantEntity(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    address = it.address,
                    city = it.city,
                    highwayOrRoute = it.highwayOrRoute,
                    cuisine = it.cuisine,
                    restaurantType = it.restaurantType,
                    overallVerdict = it.overallVerdict,
                    tags = it.tags,
                    isShared = it.isShared,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            for (r in restaurantEntities) {
                val existing = restaurantDao.getRestaurantByIdSync(r.id)
                if (existing == null || r.updatedAt > existing.updatedAt) {
                    restaurantDao.insert(r)
                }
            }

            // 2. Merge Visits
            val visitEntities = payload.visits.map {
                VisitEntryEntity(
                    id = it.id,
                    restaurantId = it.restaurantId,
                    visitDate = it.visitDate,
                    channel = it.channel,
                    visitVerdict = it.visitVerdict,
                    billAmount = it.billAmount,
                    billReceiptPhotoUri = null,
                    generalNotes = it.generalNotes,
                    createdByMemberId = it.createdByMemberId,
                    createdByMemberName = it.createdByMemberName,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            visitDao.insertAll(visitEntities)

            // 3. Merge Dishes
            val dishEntities = payload.dishes.map {
                DishFeedbackEntity(
                    id = it.id,
                    visitId = it.visitId,
                    restaurantId = it.restaurantId,
                    dishName = it.dishName,
                    category = it.category,
                    verdict = it.verdict,
                    tags = it.tags,
                    memberPreferences = it.memberPreferences,
                    price = it.price,
                    photoUris = emptyList(),
                    notes = it.notes,
                    isShared = it.isShared,
                    updatedAt = it.updatedAt,
                    isDeleted = it.isDeleted
                )
            }
            dishDao.insertAll(dishEntities)

            val now = System.currentTimeMillis()
            setLastSyncTimestamp(now)
            _syncStatus.value = SyncStatus.Success("Synced ${payload.restaurants.size} places & ${payload.dishes.size} dishes", now)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Sync failed")
        }
    }

    /**
     * Exports full database to a JSON file in app cache or storage for manual backup/sharing.
     */
    suspend fun exportToJsonFile(): File = withContext(Dispatchers.IO) {
        val payload = exportSharedPayload(0L)
        val jsonString = json.encodeToString(payload)
        val exportFile = File(context.cacheDir, "palatelog_backup_${System.currentTimeMillis()}.json")
        exportFile.writeText(jsonString)
        exportFile
    }

    /**
     * Imports from a JSON string.
     */
    suspend fun importFromJsonString(jsonString: String) {
        val payload = json.decodeFromString<SyncPayload>(jsonString)
        importPayload(payload)
    }
}
