package com.prismpixelvertex.palatelog.core.model

import java.util.UUID

enum class DishVerdict(val displayName: String) {
    GO("Must Try / Go 🟢"),
    NO_GO("Avoid / No-Go 🔴"),
    MEH("Neutral / Okay 🟡")
}

enum class DishCategory(val displayName: String) {
    MAIN("Main Course"),
    APPETIZER("Starter / Appetizer"),
    BEVERAGE("Beverage / Tea / Coffee"),
    DESSERT("Dessert"),
    SIDE("Side / Accompaniment"),
    SNACK("Snack")
}

data class DishFeedback(
    val id: String = UUID.randomUUID().toString(),
    val visitId: String,
    val restaurantId: String,
    val dishName: String,
    val category: DishCategory = DishCategory.MAIN,
    val verdict: DishVerdict = DishVerdict.GO,
    val tags: List<String> = emptyList(), // e.g. "Soggy", "Spicy", "Authentic", "Overpriced"
    val memberPreferences: Map<String, String> = emptyMap(), // MemberName -> Note/Reaction (e.g. "Wife" -> "Loved it", "Karthik" -> "Too oily")
    val price: Double = 0.0,
    val photoUris: List<String> = emptyList(),
    val notes: String = "",
    val isShared: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
