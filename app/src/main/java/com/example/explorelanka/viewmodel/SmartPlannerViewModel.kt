package com.example.explorelanka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.explorelanka.data.local.entity.SmartPlanWithStops
import com.example.explorelanka.data.planner.PlannerDraft
import com.example.explorelanka.data.planner.PlannerPlace
import com.example.explorelanka.data.planner.PlannerStop
import com.example.explorelanka.data.planner.SmartPlannerEngine
import com.example.explorelanka.data.repository.SmartPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SmartPlannerViewModel(private val repo: SmartPlanRepository) : ViewModel() {
    val savedPlans: StateFlow<List<SmartPlanWithStops>> = repo.observePlans().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    private val _draft = MutableStateFlow<PlannerDraft?>(null)
    val draft: StateFlow<PlannerDraft?> = _draft.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun clearMessage() { _message.value = null }
    fun clearDraft() { _draft.value = null }

    fun generate(
        startDate: LocalDate, days: Int, city: String, categories: Set<String>,
        budget: Long, places: List<PlannerPlace>,
        startingLatitude: Double? = null, startingLongitude: Double? = null
    ) {
        runCatching {
            SmartPlannerEngine.generate(
                startDate, days, city, categories, budget, places,
                startingLatitude = startingLatitude,
                startingLongitude = startingLongitude
            )
        }.onSuccess {
            _draft.value = it
            _message.value = "Suggested itinerary ready. Review it before saving."
        }.onFailure { _message.value = it.message ?: "Cannot create itinerary." }
    }

    fun openSaved(saved: SmartPlanWithStops) {
        runCatching { repo.convert(saved) }
            .onSuccess { _draft.value = it }
            .onFailure { _message.value = it.message }
    }

    fun rename(value: String) { _draft.value = _draft.value?.copy(title = value) }

    fun removeStop(destinationId: String) {
        _draft.value = _draft.value?.let { draft ->
            draft.copy(stops = SmartPlannerEngine.normalizedStops(
                draft.stops.filterNot { it.destinationId == destinationId }
            ))
        }
    }

    fun addStop(place: PlannerPlace, dayIndex: Int) {
        _draft.value = _draft.value?.let { draft ->
            if (draft.stops.any { it.destinationId == place.id }) {
                _message.value = "This destination is already in the itinerary."
                draft
            } else if (dayIndex !in 0 until draft.numberOfDays) draft else {
                draft.copy(stops = SmartPlannerEngine.normalizedStops(
                    draft.stops + PlannerStop(place.id, dayIndex, Int.MAX_VALUE,
                        SmartPlannerEngine.admissionEstimate(place.category))
                ))
            }
        }
    }

    /** Move between days; the order within each day remains deterministic. */
    fun moveDay(destinationId: String, delta: Int) {
        _draft.value = _draft.value?.let { draft ->
            draft.copy(stops = SmartPlannerEngine.normalizedStops(
                draft.stops.map { stop ->
                    if (stop.destinationId == destinationId) {
                        stop.copy(dayIndex = (stop.dayIndex + delta).coerceIn(0, draft.numberOfDays - 1),
                            position = Int.MAX_VALUE)
                    } else stop
                }
            ))
        }
    }

    fun saveDraft() {
        val toSave = _draft.value ?: return
        if (_busy.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _busy.value = true
            try {
                val id = repo.save(toSave)
                // Prevent a second tap from inserting a duplicate new plan.
                _draft.value = toSave.copy(id = id)
                _message.value = "Trip plan saved offline on this device."
            } catch (e: Exception) {
                _message.value = e.message ?: "Unable to save plan."
            } finally {
                _busy.value = false
            }
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.delete(id)
                if (_draft.value?.id == id) _draft.value = null
                _message.value = "Saved plan deleted."
            } catch (e: Exception) {
                _message.value = e.message ?: "Unable to delete the plan."
            }
        }
    }

    class Factory(private val repo: SmartPlanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SmartPlannerViewModel(repo) as T
    }
}
