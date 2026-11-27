package com.example.explorelanka.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TripWithStops(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val stops: List<TripStopEntity>
)
