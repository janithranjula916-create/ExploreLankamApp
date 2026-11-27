package com.example.explorelanka.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.explorelanka.data.local.entity.WeatherCacheEntity
import com.example.explorelanka.data.repository.WeatherRepository
import com.example.explorelanka.data.weather.PlaceCoordinates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val placeId: String,
    private val coordinates: PlaceCoordinates
) : ViewModel() {
    val weather = repository.observe(placeId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun refresh() {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repository.refresh(placeId, coordinates.latitude, coordinates.longitude)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = "Unable to update weather. Check your connection and retry."
                android.util.Log.w("ExploreLankaWeather", "Weather request failed", exception)
            } finally {
                isLoading = false
            }
        }
    }

    companion object {
        fun factory(
            repository: WeatherRepository,
            placeId: String,
            coordinates: PlaceCoordinates
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(WeatherViewModel::class.java))
                return WeatherViewModel(repository, placeId, coordinates) as T
            }
        }
    }
}
