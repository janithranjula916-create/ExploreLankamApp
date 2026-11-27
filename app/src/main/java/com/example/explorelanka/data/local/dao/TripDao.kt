package com.example.explorelanka.data.local.dao

import androidx.room.*
import com.example.explorelanka.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Transaction
    @Query("SELECT * FROM trips WHERE ownerId = :uid AND isDeleted = 0 ORDER BY startDate ASC, createdAt DESC")
    fun observeTrips(uid: String): Flow<List<TripWithStops>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun findTrip(tripId: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :tripId AND ownerId = :uid LIMIT 1")
    suspend fun findOwnedTrip(tripId: String, uid: String): TripEntity?

    @Query("SELECT * FROM trips WHERE ownerId = :uid AND syncStatus = 'PENDING' ORDER BY updatedAt ASC")
    fun observePending(uid: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE ownerId = :uid AND syncStatus = 'PENDING' ORDER BY updatedAt ASC")
    suspend fun getPending(uid: String): List<TripEntity>

    @Query("SELECT * FROM trip_stops WHERE tripId = :tripId ORDER BY position")
    suspend fun getStops(tripId: String): List<TripStopEntity>

    @Upsert
    suspend fun upsertTrip(trip: TripEntity)

    @Query("DELETE FROM trip_stops WHERE tripId = :tripId")
    suspend fun clearStops(tripId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<TripStopEntity>)

    @Query("UPDATE trips SET syncStatus = 'SYNCED' WHERE id = :tripId AND ownerId = :uid AND updatedAt = :version AND syncStatus = 'PENDING'")
    suspend fun markSynced(tripId: String, uid: String, version: Long)

    @Query("UPDATE trips SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :version WHERE id = :tripId AND ownerId = :uid")
    suspend fun markDeleted(tripId: String, uid: String, version: Long)

    @Query("SELECT COUNT(*) FROM trips WHERE ownerId = 'local' AND isDeleted = 0")
    suspend fun countLegacyTrips(): Int

    @Query("UPDATE trips SET ownerId = :uid, syncStatus = 'PENDING' WHERE ownerId = 'local'")
    suspend fun importLegacyTrips(uid: String)
}
