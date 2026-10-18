package com.karthik.palatelog.core.model

import java.util.UUID

enum class Verdict {
    GO,       // 🟢 Must visit / Recommended
    NO_GO,    // 🔴 Never again / Avoid
    CAUTION   // 🟡 Mixed experience / Conditional (e.g. good food, bad restrooms)
}

enum class RestaurantType(val displayName: String) {
    DINE_IN("Dine-In"),
    DHABA("Highway Dhaba"),
    CAFE("Café"),
    CLOUD_KITCHEN("Cloud Kitchen"),
    DRIVE_THRU("Drive-Thru"),
    STREET_FOOD("Street Food / Stall")
}

data class Restaurant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val city: String = "",
    val highwayOrRoute: String = "",
    val cuisine: String = "",
    val restaurantType: RestaurantType = RestaurantType.DINE_IN,
    val overallVerdict: Verdict = Verdict.GO,
    val tags: List<String> = emptyList(),
    val isShared: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
