package com.example.explorelanka.data.repository

import android.content.Context
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.JournalPhotoEntity
import com.example.explorelanka.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class JournalRepository(
    private val appContext: Context,
    database: ExploreLankaDatabase
) {
    private val photos = database.journalDao()
    private val scans = database.scanHistoryDao()

    private fun ownDirectory(ownerId: String): File {
        require(ownerId.isNotBlank())
        val safeUser = ownerId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(appContext.filesDir, "travel_journal/$safeUser")
            .apply { mkdirs() }
    }

    fun newCaptureFile(ownerId: String): File =
        File(ownDirectory(ownerId), "${UUID.randomUUID()}.jpg")

    fun observePhotos(ownerId: String): Flow<List<JournalPhotoEntity>> =
        photos.observePhotos(ownerId)

    fun recentScans(ownerId: String): Flow<List<ScanHistoryEntity>> =
        scans.recentScans(ownerId)

    suspend fun storePhoto(
        ownerId: String,
        destinationId: String,
        caption: String,
        file: File
    ) {
        require(file.isFile) { "Captured photograph was not saved." }
        require(file.canonicalFile.parentFile == ownDirectory(ownerId).canonicalFile) {
            "Photo location is not in this account's private journal."
        }
        photos.insert(
            JournalPhotoEntity(
                id = UUID.randomUUID().toString(),
                ownerId = ownerId,
                destinationId = destinationId,
                localPath = file.absolutePath,
                caption = caption.trim().take(240),
                capturedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateCaption(ownerId: String, photoId: String, caption: String) {
        photos.updateCaption(photoId, ownerId, caption.trim().take(240))
    }

    suspend fun deletePhoto(ownerId: String, photoId: String) {
        val photo = photos.findOwned(photoId, ownerId) ?: return
        photos.deleteOwned(photoId, ownerId)
        val file = File(photo.localPath).canonicalFile
        if (file.parentFile == ownDirectory(ownerId).canonicalFile) {
            file.delete()
        }
    }

    suspend fun recordScan(ownerId: String, destinationId: String) {
        scans.insert(
            ScanHistoryEntity(
                ownerId = ownerId,
                destinationId = destinationId,
                scannedAtMillis = System.currentTimeMillis()
            )
        )
    }
}
