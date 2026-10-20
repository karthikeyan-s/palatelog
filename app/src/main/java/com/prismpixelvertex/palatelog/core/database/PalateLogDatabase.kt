package com.prismpixelvertex.palatelog.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        RestaurantEntity::class,
        VisitEntryEntity::class,
        DishFeedbackEntity::class,
        FamilyMemberEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PalateLogDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao
    abstract fun visitDao(): VisitDao
    abstract fun dishFeedbackDao(): DishFeedbackDao
    abstract fun familyMemberDao(): FamilyMemberDao
}
