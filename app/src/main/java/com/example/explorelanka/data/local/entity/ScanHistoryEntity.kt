package com.example.explorelanka.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history", indices = [Index("ownerId")])
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val destinationId: String,
    val scannedAtMillis: Long
)
