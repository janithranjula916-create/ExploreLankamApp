package com.example.explorelanka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Each destination may occur once in each trip. Deleting the trip removes its stops. */
@Entity(
    tableName = "trip_stops",
    primaryKeys = ["tripId", "destinationId"],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class TripStopEntity(
    val tripId: String,
    val destinationId: String,
    val position: Int
)
