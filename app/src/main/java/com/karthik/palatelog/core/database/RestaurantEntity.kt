package com.karthik.palatelog.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.karthik.palatelog.core.model.Restaurant
import com.karthik.palatelog.core.model.RestaurantType
import com.karthik.palatelog.core.model.Verdict

@Entity(
    tableName = "restaurants",
    indices = [
        Index(value = ["latitude", "longitude"]),
        Index(value = ["name"]),
        Index(value = ["city"]),
        Index(value = ["overallVerdict"])
    ]
)
data class RestaurantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val highwayOrRoute: String,
    val cuisine: String,
    val restaurantType: String,
    val overallVerdict: String,
    val tags: List<String>,
    val isShared: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
) {
    fun toDomain(): Restaurant = Restaurant(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        city = city,
        highwayOrRoute = highwayOrRoute,
        cuisine = cuisine,
        restaurantType = try { RestaurantType.valueOf(restaurantType) } catch (e: Exception) { RestaurantType.DINE_IN },
        overallVerdict = try { Verdict.valueOf(overallVerdict) } catch (e: Exception) { Verdict.GO },
        tags = tags,
        isShared = isShared,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    companion object {
        fun fromDomain(model: Restaurant): RestaurantEntity = RestaurantEntity(
            id = model.id,
            name = model.name,
            latitude = model.latitude,
            longitude = model.longitude,
            address = model.address,
            city = model.city,
            highwayOrRoute = model.highwayOrRoute,
            cuisine = model.cuisine,
            restaurantType = model.restaurantType.name,
            overallVerdict = model.overallVerdict.name,
            tags = model.tags,
            isShared = model.isShared,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            isDeleted = model.isDeleted
        )
    }
}
