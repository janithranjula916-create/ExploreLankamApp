package com.example.explorelanka.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trips", indices = [Index(value = ["ownerId"])])
data class TripEntity(
    @PrimaryKey val id: String,
    val ownerId: String = "local",
    val title: String,
    val startDate: String,
    val endDate: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "'PENDING'") val syncStatus: String = "PENDING",
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false
)
