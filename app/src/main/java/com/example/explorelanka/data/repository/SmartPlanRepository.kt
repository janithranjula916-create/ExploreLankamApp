package com.example.explorelanka.data.repository

import androidx.room.withTransaction
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.SmartPlanEntity
import com.example.explorelanka.data.local.entity.SmartPlanStopEntity
import com.example.explorelanka.data.local.entity.SmartPlanWithStops
import com.example.explorelanka.data.planner.PlannerDraft
import com.example.explorelanka.data.planner.PlannerStop
import com.example.explorelanka.data.planner.SmartPlannerEngine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

class SmartPlanRepository(
    private val db: ExploreLankaDatabase,
    private val ownerId: String
) {
    private val dao = db.smartPlanDao()

    fun observePlans(): Flow<List<SmartPlanWithStops>> = dao.observeOwnedPlans(ownerId)

    suspend fun save(draft: PlannerDraft): String = db.withTransaction {
        require(draft.title.trim().length in 3..80) { "Enter a trip name (3–80 characters)." }
        require(draft.numberOfDays in 1..10) { "Choose 1 to 10 days." }
        require(draft.budgetLkr > 0L) { "Enter a budget above zero." }
        require(draft.stops.isNotEmpty()) { "Add at least one destination before saving." }
        require(draft.stops.map { it.destinationId }.distinct().size == draft.stops.size) {
            "Each destination can appear only once in a plan."
        }
        require(draft.stops.all { it.dayIndex in 0 until draft.numberOfDays }) {
            "A stop is assigned to an invalid day."
        }
        val old = draft.id?.let { dao.findOwnedPlan(it, ownerId)?.plan }
        if (draft.id != null && old == null) error("This plan does not belong to your account.")
        val now = System.currentTimeMillis()
        val id = draft.id ?: UUID.randomUUID().toString()
        val ordered = SmartPlannerEngine.normalizedStops(draft.stops)
        dao.upsertPlan(
            SmartPlanEntity(
                id = id,
                ownerId = ownerId,
                title = draft.title.trim(),
                startDate = draft.startDate.toString(),
                numberOfDays = draft.numberOfDays,
                startingCity = draft.startingCity,
                startingLatitude = draft.startingLatitude,
                startingLongitude = draft.startingLongitude,
                interestsCsv = draft.interests.sorted().joinToString(","),
                budgetLkr = draft.budgetLkr,
                estimatedTotalLkr = SmartPlannerEngine.totalEstimate(draft.numberOfDays, ordered),
                createdAtMillis = old?.createdAtMillis ?: now,
                updatedAtMillis = maxOf(now, (old?.updatedAtMillis ?: 0L) + 1L)
            )
        )
        dao.deleteStops(id)
        dao.insertStops(ordered.map { stop ->
            SmartPlanStopEntity(
                planId = id,
                destinationId = stop.destinationId,
                dayIndex = stop.dayIndex,
                position = stop.position,
                estimatedAdmissionLkr = stop.estimatedAdmissionLkr
            )
        })
        id
    }

    suspend fun delete(id: String) = db.withTransaction {
        dao.deleteOwnedPlan(id, ownerId)
    }

    fun convert(saved: SmartPlanWithStops): PlannerDraft {
        check(saved.plan.ownerId == ownerId) { "Another account's plan cannot be opened." }
        return PlannerDraft(
            id = saved.plan.id,
            title = saved.plan.title,
            startDate = LocalDate.parse(saved.plan.startDate),
            numberOfDays = saved.plan.numberOfDays,
            startingCity = saved.plan.startingCity,
            startingLatitude = saved.plan.startingLatitude,
            startingLongitude = saved.plan.startingLongitude,
            interests = saved.plan.interestsCsv.split(',').filter { it.isNotBlank() }.toSet(),
            budgetLkr = saved.plan.budgetLkr,
            stops = saved.stops.sortedWith(compareBy({ it.dayIndex }, { it.position })).map {
                PlannerStop(it.destinationId, it.dayIndex, it.position, it.estimatedAdmissionLkr)
            }
        )
    }
}
