package com.example.explorelanka.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SmartPlanWithStops(
    @Embedded val plan: SmartPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val stops: List<SmartPlanStopEntity>
)
