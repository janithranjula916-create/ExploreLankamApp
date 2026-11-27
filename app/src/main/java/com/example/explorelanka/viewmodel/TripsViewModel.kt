package com.example.explorelanka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.TripWithStops
import com.example.explorelanka.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Input fields come from the trip editor; IDs are stable across edits. */
data class TripDraft(
    val id: String?,
    val title: String,
    val startDate: String,
    val endDate: String,
    val notes: String,
    val destinationIds: List<String>
)

data class TripsUiState(
    val loading: Boolean = true,
    val trips: List<TripWithStops> = emptyList()
)

class TripsViewModel(private val repository: TripRepository) : ViewModel() {
    val uiState: StateFlow<TripsUiState> = repository.observeTrips()
        .map { TripsUiState(loading = false, trips = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun saveTrip(draft: TripDraft) {
        val title = draft.title.trim()
        val start = runCatching { LocalDate.parse(draft.startDate.trim()) }.getOrNull()
        val end = runCatching { LocalDate.parse(draft.endDate.trim()) }.getOrNull()
        when {
            title.isBlank() -> { _message.value = "Enter a trip name."; return }
            title.length > 80 -> { _message.value = "Trip name must be 80 characters or less."; return }
            start == null || end == null -> { _message.value = "Dates must use YYYY-MM-DD."; return }
            end.isBefore(start) -> { _message.value = "End date must not precede start date."; return }
            draft.destinationIds.isEmpty() -> { _message.value = "Select at least one destination."; return }
        }
        viewModelScope.launch {
            try {
                repository.saveTrip(
                    draft.id, title, start.toString(), end.toString(),
                    draft.notes.trim(), draft.destinationIds
                )
                _message.value = if (draft.id == null) "Trip created." else "Trip updated."
            } catch (e: Exception) {
                _message.value = "Couldn't save trip: ${e.localizedMessage ?: "database error"}"
            }
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteTrip(id)
                _message.value = "Trip deleted."
            } catch (e: Exception) {
                _message.value = "Couldn't delete trip: ${e.localizedMessage ?: "database error"}"
            }
        }
    }

    fun clearMessage() { _message.value = null }

    companion object {
        fun factory(context: Context, userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TripsViewModel::class.java)) {
                    "Unknown ViewModel: ${modelClass.name}"
                }
                return TripsViewModel(
                    TripRepository(ExploreLankaDatabase.getInstance(context.applicationContext), userId)
                ) as T
            }
        }
    }
}
