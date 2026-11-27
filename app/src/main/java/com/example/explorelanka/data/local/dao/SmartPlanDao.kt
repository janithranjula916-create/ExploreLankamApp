package com.example.explorelanka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.explorelanka.data.local.entity.SmartPlanEntity
import com.example.explorelanka.data.local.entity.SmartPlanStopEntity
import com.example.explorelanka.data.local.entity.SmartPlanWithStops
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartPlanDao {
    @Transaction
    @Query("SELECT * FROM smart_plans WHERE ownerId = :ownerId ORDER BY updatedAtMillis DESC")
    fun observeOwnedPlans(ownerId: String): Flow<List<SmartPlanWithStops>>

    @Transaction
    @Query("SELECT * FROM smart_plans WHERE id = :id AND ownerId = :ownerId LIMIT 1")
    suspend fun findOwnedPlan(id: String, ownerId: String): SmartPlanWithStops?

    @Upsert
    suspend fun upsertPlan(plan: SmartPlanEntity)

    @Query("DELETE FROM smart_plan_stops WHERE planId = :planId")
    suspend fun deleteStops(planId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<SmartPlanStopEntity>)

    @Query("DELETE FROM smart_plans WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteOwnedPlan(id: String, ownerId: String)
}
