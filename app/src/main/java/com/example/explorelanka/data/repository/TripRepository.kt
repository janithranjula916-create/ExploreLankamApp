package com.example.explorelanka.data.repository

import androidx.room.withTransaction
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.TripEntity
import com.example.explorelanka.data.local.entity.TripStopEntity
import com.example.explorelanka.data.local.entity.TripWithStops
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TripRepository(
    private val database: ExploreLankaDatabase,
    private val ownerId: String
) {
    private val dao = database.tripDao()

    fun observeTrips(): Flow<List<TripWithStops>> = dao.observeTrips(ownerId)

    suspend fun saveTrip(
        id: String?, title: String, startDate: String, endDate: String,
        notes: String, destinationIds: List<String>
    ) {
        database.withTransaction {
            val existing = id?.let { dao.findOwnedTrip(it, ownerId) }
            if (id != null && existing == null) error("This trip does not belong to this account.")
            val now = System.currentTimeMillis()
            val updated = maxOf(now, (existing?.updatedAt ?: 0L) + 1L)
            val tripId = id ?: UUID.randomUUID().toString()
            dao.upsertTrip(
                TripEntity(
                    id = tripId, ownerId = ownerId, title = title,
                    startDate = startDate, endDate = endDate, notes = notes,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = updated, syncStatus = "PENDING", isDeleted = false
                )
            )
            dao.clearStops(tripId)
            dao.insertStops(destinationIds.distinct().mapIndexed { index, destinationId ->
                TripStopEntity(tripId, destinationId, index)
            })
        }
    }

    suspend fun deleteTrip(id: String) {
        database.withTransaction {
            val trip = dao.findOwnedTrip(id, ownerId) ?: return@withTransaction
            dao.markDeleted(id, ownerId, maxOf(System.currentTimeMillis(), trip.updatedAt + 1))
        }
    }

    suspend fun countLegacyTrips(): Int = dao.countLegacyTrips()

    suspend fun importLegacyTrips() {
        database.withTransaction { dao.importLegacyTrips(ownerId) }
    }
}
