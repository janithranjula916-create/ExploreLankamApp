package com.example.explorelanka.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Separate from My Trips: smart itineraries are stored on this device only. */
@Entity(tableName = "smart_plans", indices = [Index(value = ["ownerId"])])
data class SmartPlanEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    val startDate: String,
    val numberOfDays: Int,
    val startingCity: String,
    val interestsCsv: String,
    val budgetLkr: Long,
    val estimatedTotalLkr: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val startingLatitude: Double? = null,
    val startingLongitude: Double? = null
)
