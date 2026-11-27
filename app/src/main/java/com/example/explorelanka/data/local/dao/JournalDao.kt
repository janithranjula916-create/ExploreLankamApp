package com.example.explorelanka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.explorelanka.data.local.entity.JournalPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_photos WHERE ownerId = :ownerId ORDER BY capturedAtMillis DESC")
    fun observePhotos(ownerId: String): Flow<List<JournalPhotoEntity>>

    @Insert
    suspend fun insert(photo: JournalPhotoEntity)

    @Query("SELECT * FROM journal_photos WHERE id = :id AND ownerId = :ownerId LIMIT 1")
    suspend fun findOwned(id: String, ownerId: String): JournalPhotoEntity?

    @Query("UPDATE journal_photos SET caption = :caption WHERE id = :id AND ownerId = :ownerId")
    suspend fun updateCaption(id: String, ownerId: String, caption: String): Int

    @Query("DELETE FROM journal_photos WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteOwned(id: String, ownerId: String): Int
}
