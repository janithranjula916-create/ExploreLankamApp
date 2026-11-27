package com.example.explorelanka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.explorelanka.data.location.LocationRepository
import com.example.explorelanka.data.location.UserPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NearbyUiState(
    val position: UserPosition? = null,
    val loading: Boolean = false,
    val message: String? = null
)

class NearbyViewModel(private val repository: LocationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = mutableUiState.asStateFlow()

    fun findMyLocation() {
        if (mutableUiState.value.loading) return
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(loading = true, message = null)
            try {
                val current = repository.getCurrentPosition()
                mutableUiState.value = NearbyUiState(
                    position = current,
                    message = if (current == null) {
                        "Could not get a current position. Turn on device Location and try again."
                    } else null
                )
            } catch (_: SecurityException) {
                mutableUiState.value = NearbyUiState(
                    message = "Location access is disabled. You can still browse the map."
                )
            } catch (_: Exception) {
                mutableUiState.value = NearbyUiState(
                    message = "Location is unavailable. Check Google Play services and try again."
                )
            }
        }
    }

    fun onPermissionDenied() {
        mutableUiState.value = mutableUiState.value.copy(
            loading = false,
            message = "Permission denied. The destination map is still available without GPS."
        )
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NearbyViewModel::class.java)) {
                "Unknown ViewModel type: ${modelClass.name}"
            }
            return NearbyViewModel(LocationRepository(context.applicationContext)) as T
        }
    }
}
