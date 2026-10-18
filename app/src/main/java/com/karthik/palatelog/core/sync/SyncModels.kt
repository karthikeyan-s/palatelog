package com.karthik.palatelog.core.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(
    val version: Int = 1,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val restaurants: List<SyncRestaurant> = emptyList(),
    val visits: List<SyncVisit> = emptyList(),
    val dishes: List<SyncDish> = emptyList()
)

@Serializable
data class SyncRestaurant(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val city: String = "",
    val highwayOrRoute: String = "",
    val cuisine: String = "",
    val restaurantType: String,
    val overallVerdict: String,
    val tags: List<String> = emptyList(),
    val isShared: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class SyncVisit(
    val id: String,
    val restaurantId: String,
    val visitDate: Long,
    val channel: String,
    val visitVerdict: String,
    val billAmount: Double = 0.0,
    val generalNotes: String = "",
    val createdByMemberId: String = "",
    val createdByMemberName: String = "",
    val isShared: Boolean = true,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class SyncDish(
    val id: String,
    val visitId: String,
    val restaurantId: String,
    val dishName: String,
    val category: String,
    val verdict: String,
    val tags: List<String> = emptyList(),
    val memberPreferences: Map<String, String> = emptyMap(),
    val price: Double = 0.0,
    val notes: String = "",
    val isShared: Boolean = true,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
