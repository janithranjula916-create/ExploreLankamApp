package com.example.explorelanka.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Image bytes are held in app-private storage; Room stores only metadata. */
@Entity(
    tableName = "journal_photos",
    indices = [Index("ownerId"), Index("destinationId")]
)
data class JournalPhotoEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val destinationId: String,
    val localPath: String,
    val caption: String,
    val capturedAtMillis: Long
)
