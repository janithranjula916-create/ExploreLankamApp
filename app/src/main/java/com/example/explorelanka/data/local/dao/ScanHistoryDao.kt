package com.example.explorelanka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.explorelanka.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(entry: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history WHERE ownerId = :ownerId ORDER BY scannedAtMillis DESC LIMIT 30")
    fun recentScans(ownerId: String): Flow<List<ScanHistoryEntity>>
}
