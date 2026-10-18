package com.karthik.palatelog.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visit_entries WHERE isDeleted = 0 ORDER BY visitDate DESC")
    fun getAllVisits(): Flow<List<VisitEntryEntity>>

    @Query("SELECT * FROM visit_entries WHERE restaurantId = :restaurantId AND isDeleted = 0 ORDER BY visitDate DESC")
    fun getVisitsForRestaurant(restaurantId: String): Flow<List<VisitEntryEntity>>

    @Query("SELECT * FROM visit_entries WHERE id = :id AND isDeleted = 0")
    fun getVisitById(id: String): Flow<VisitEntryEntity?>

    @Query("SELECT * FROM visit_entries WHERE updatedAt > :sinceTimestamp")
    suspend fun getModifiedSince(sinceTimestamp: Long): List<VisitEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: VisitEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<VisitEntryEntity>)

    @Update
    suspend fun update(visit: VisitEntryEntity)

    @Query("UPDATE visit_entries SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())
}
