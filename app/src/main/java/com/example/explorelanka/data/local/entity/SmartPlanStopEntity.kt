package com.example.explorelanka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "smart_plan_stops",
    primaryKeys = ["planId", "destinationId"],
    foreignKeys = [ForeignKey(
        entity = SmartPlanEntity::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["planId"]), Index(value = ["planId", "dayIndex"])]
)
data class SmartPlanStopEntity(
    val planId: String,
    val destinationId: String,
    val dayIndex: Int,
    val position: Int,
    val estimatedAdmissionLkr: Long
)
