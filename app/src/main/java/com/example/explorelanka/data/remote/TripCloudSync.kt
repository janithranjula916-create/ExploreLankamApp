package com.example.explorelanka.data.remote

import android.util.Log
import androidx.room.withTransaction
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.TripEntity
import com.example.explorelanka.data.local.entity.TripStopEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Simple offline-first demonstration sync:
 * - Room is the local source of truth.
 * - PENDING rows (including soft deletions) are sent to the account's Firestore path.
 * - Incoming snapshots never overwrite local PENDING edits.
 * - Higher updatedAt wins for otherwise synced rows (device clocks must be reasonably correct).
 * - Firestore deletion tombstones are retained to propagate deletions to other devices.
 * Production systems need server-authoritative conflict/version handling and durable workers.
 */
class TripCloudSync(private val database: ExploreLankaDatabase) {
    private val firestore = FirebaseFirestore.getInstance()
    private val dao = database.tripDao()
    private val uploadMutex = Mutex()

    fun start(scope: CoroutineScope, userId: String): () -> Unit {
        val collection = firestore.collection("users").document(userId).collection("trips")

        val listener: ListenerRegistration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("ExploreLankaSync", "Firestore trip listener failed", error)
                return@addSnapshotListener
            }
            val docs = snapshot?.documents.orEmpty()
            scope.launch {
                docs.forEach { doc ->
                    // Do not merge our own local unacknowledged Firestore writes.
                    if (doc.metadata.hasPendingWrites()) return@forEach
                    try {
                        val title = doc.getString("title") ?: return@forEach
                        val version = doc.getLong("updatedAt") ?: return@forEach
                        val tripId = doc.id
                        val deleted = doc.getBoolean("deleted") ?: false
                        val destinations = (doc.get("destinationIds") as? List<*>)
                            ?.filterIsInstance<String>().orEmpty()
                        database.withTransaction {
                            val existing = dao.findTrip(tripId)
                            if (existing != null && existing.ownerId != userId) return@withTransaction
                            if (existing != null && existing.syncStatus == "PENDING") return@withTransaction
                            if (existing != null && version <= existing.updatedAt) return@withTransaction
                            dao.upsertTrip(TripEntity(
                                id = tripId,
                                ownerId = userId,
                                title = title,
                                startDate = doc.getString("startDate").orEmpty(),
                                endDate = doc.getString("endDate").orEmpty(),
                                notes = doc.getString("notes").orEmpty(),
                                createdAt = doc.getLong("createdAt") ?: version,
                                updatedAt = version,
                                syncStatus = "SYNCED",
                                isDeleted = deleted
                            ))
                            dao.clearStops(tripId)
                            if (!deleted) {
                                dao.insertStops(destinations.distinct().mapIndexed { index, destinationId ->
                                    TripStopEntity(tripId, destinationId, index)
                                })
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ExploreLankaSync", "Couldn't merge a remote trip", e)
                    }
                }
            }
        }

        suspend fun uploadPending() = uploadMutex.withLock {
            for (trip in dao.getPending(userId)) {
                try {
                    val stops = dao.getStops(trip.id).sortedBy { it.position }
                        .map { it.destinationId }
                    val data = mapOf(
                        "id" to trip.id,
                        "title" to trip.title,
                        "startDate" to trip.startDate,
                        "endDate" to trip.endDate,
                        "notes" to trip.notes,
                        "createdAt" to trip.createdAt,
                        "updatedAt" to trip.updatedAt,
                        "deleted" to trip.isDeleted,
                        "destinationIds" to stops
                    )
                    collection.document(trip.id).set(data).await()
                    dao.markSynced(trip.id, userId, trip.updatedAt)
                } catch (e: Exception) {
                    Log.w("ExploreLankaSync", "Upload pending; retry after reconnection", e)
                }
            }
        }

        // Room emits immediately and after every local edit. A periodic retry also
        // handles transient backend failures that did not cause another Room change.
        val outgoing: Job = scope.launch {
            dao.observePending(userId).collectLatest { pending ->
                if (pending.isNotEmpty()) uploadPending()
            }
        }
        val retry: Job = scope.launch {
            while (isActive) {
                delay(30_000)
                uploadPending()
            }
        }

        return {
            listener.remove()
            outgoing.cancel()
            retry.cancel()
        }
    }
}
