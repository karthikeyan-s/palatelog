package com.karthik.palatelog.di

import android.content.Context
import androidx.room.Room
import com.karthik.palatelog.core.database.DishFeedbackDao
import com.karthik.palatelog.core.database.FamilyMemberDao
import com.karthik.palatelog.core.database.PalateLogDatabase
import com.karthik.palatelog.core.database.RestaurantDao
import com.karthik.palatelog.core.database.VisitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePalateLogDatabase(
        @ApplicationContext context: Context
    ): PalateLogDatabase {
        return Room.databaseBuilder(
            context,
            PalateLogDatabase::class.java,
            "palatelog.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRestaurantDao(database: PalateLogDatabase): RestaurantDao = database.restaurantDao()

    @Provides
    fun provideVisitDao(database: PalateLogDatabase): VisitDao = database.visitDao()

    @Provides
    fun provideDishFeedbackDao(database: PalateLogDatabase): DishFeedbackDao = database.dishFeedbackDao()

    @Provides
    fun provideFamilyMemberDao(database: PalateLogDatabase): FamilyMemberDao = database.familyMemberDao()
}
