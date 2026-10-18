package com.karthik.palatelog.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karthik.palatelog.core.model.DishCategory
import com.karthik.palatelog.core.model.DishFeedback
import com.karthik.palatelog.core.model.DishVerdict

@Entity(
    tableName = "dish_feedbacks",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VisitEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["restaurantId"]),
        Index(value = ["visitId"]),
        Index(value = ["dishName"]),
        Index(value = ["verdict"])
    ]
)
data class DishFeedbackEntity(
    @PrimaryKey
    val id: String,
    val visitId: String,
    val restaurantId: String,
    val dishName: String,
    val category: String,
    val verdict: String,
    val tags: List<String>,
    val memberPreferences: Map<String, String>,
    val price: Double,
    val photoUris: List<String>,
    val notes: String,
    val isShared: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean
) {
    fun toDomain(): DishFeedback = DishFeedback(
        id = id,
        visitId = visitId,
        restaurantId = restaurantId,
        dishName = dishName,
        category = try { DishCategory.valueOf(category) } catch (e: Exception) { DishCategory.MAIN },
        verdict = try { DishVerdict.valueOf(verdict) } catch (e: Exception) { DishVerdict.GO },
        tags = tags,
        memberPreferences = memberPreferences,
        price = price,
        photoUris = photoUris,
        notes = notes,
        isShared = isShared,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    companion object {
        fun fromDomain(model: DishFeedback): DishFeedbackEntity = DishFeedbackEntity(
            id = model.id,
            visitId = model.visitId,
            restaurantId = model.restaurantId,
            dishName = model.dishName,
            category = model.category.name,
            verdict = model.verdict.name,
            tags = model.tags,
            memberPreferences = model.memberPreferences,
            price = model.price,
            photoUris = model.photoUris,
            notes = model.notes,
            isShared = model.isShared,
            updatedAt = model.updatedAt,
            isDeleted = model.isDeleted
        )
    }
}
