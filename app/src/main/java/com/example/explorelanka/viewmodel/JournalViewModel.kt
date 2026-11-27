package com.example.explorelanka.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalRepository(
        application.applicationContext,
        ExploreLankaDatabase.getInstance(application)
    )

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun dismissError() { errorMessage = null }

    fun photosFor(ownerId: String) = repository.observePhotos(ownerId)
    fun scansFor(ownerId: String) = repository.recentScans(ownerId)
    fun newCaptureFile(ownerId: String): File = repository.newCaptureFile(ownerId)

    fun saveCapture(
        ownerId: String, destinationId: String, caption: String,
        file: File, onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            runCatching { repository.storePhoto(ownerId, destinationId, caption, file) }
                .onSuccess { onSaved() }
                .onFailure { exception ->
                    file.delete()
                    errorMessage = exception.message ?: "Could not save the photograph."
                }
        }
    }

    fun editCaption(ownerId: String, photoId: String, caption: String) {
        viewModelScope.launch {
            runCatching { repository.updateCaption(ownerId, photoId, caption) }
                .onFailure { errorMessage = it.message ?: "Could not update caption." }
        }
    }

    fun deletePhoto(ownerId: String, photoId: String) {
        viewModelScope.launch {
            runCatching { repository.deletePhoto(ownerId, photoId) }
                .onFailure { errorMessage = it.message ?: "Could not delete photograph." }
        }
    }

    fun recordScan(ownerId: String, destinationId: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            // Navigation remains possible even if scan-history persistence fails.
            runCatching { repository.recordScan(ownerId, destinationId) }
                .onFailure { errorMessage = "Scan history could not be saved." }
            onSaved()
        }
    }
}
