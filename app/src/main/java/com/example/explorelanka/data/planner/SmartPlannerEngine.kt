package com.example.explorelanka.data.planner

import java.time.LocalDate
import kotlin.math.*

/** Pure Kotlin engine: deterministic, testable, no AI API or routing charges. */
data class PlannerPlace(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double
)

data class PlannerCity(val name: String, val latitude: Double, val longitude: Double)

data class PlannerStop(
    val destinationId: String,
    val dayIndex: Int,
    val position: Int,
    val estimatedAdmissionLkr: Long
)

data class PlannerDraft(
    val id: String? = null,
    val title: String,
    val startDate: LocalDate,
    val numberOfDays: Int,
    val startingCity: String,
    val startingLatitude: Double? = null,
    val startingLongitude: Double? = null,
    val interests: Set<String>,
    val budgetLkr: Long,
    val stops: List<PlannerStop>
)

object SmartPlannerEngine {
    /** Offline starting-point catalogue; coordinates approximate town centres. */
    val cities: List<PlannerCity> = listOf(
        PlannerCity("Ampara", 7.2980, 81.6750),
        PlannerCity("Ambalangoda", 6.2360, 80.0530),
        PlannerCity("Anuradhapura", 8.3120, 80.4030),
        PlannerCity("Arugam Bay", 6.8410, 81.8350),
        PlannerCity("Avissawella", 6.9540, 80.2090),
        PlannerCity("Badulla", 6.9930, 81.0550),
        PlannerCity("Balangoda", 6.6620, 80.7010),
        PlannerCity("Bandarawela", 6.8290, 80.9860),
        PlannerCity("Batticaloa", 7.7180, 81.7000),
        PlannerCity("Bentota", 6.4210, 79.9990),
        PlannerCity("Beruwala", 6.4790, 79.9820),
        PlannerCity("Chilaw", 7.5750, 79.7950),
        PlannerCity("Colombo", 6.9271, 79.8612),
        PlannerCity("Dambulla", 7.8742, 80.6511),
        PlannerCity("Dehiwala", 6.8510, 79.8660),
        PlannerCity("Ella", 6.8720, 81.0460),
        PlannerCity("Embilipitiya", 6.3440, 80.8480),
        PlannerCity("Galle", 6.0535, 80.2210),
        PlannerCity("Habarana", 8.0380, 80.7510),
        PlannerCity("Hambantota", 6.1240, 81.1190),
        PlannerCity("Haputale", 6.7690, 80.9580),
        PlannerCity("Hatton", 6.8910, 80.5960),
        PlannerCity("Hikkaduwa", 6.1410, 80.1010),
        PlannerCity("Homagama", 6.8410, 80.0030),
        PlannerCity("Jaffna", 9.6615, 80.0255),
        PlannerCity("Kalpitiya", 8.2300, 79.7660),
        PlannerCity("Kalutara", 6.5850, 79.9610),
        PlannerCity("Kandy", 7.2906, 80.6337),
        PlannerCity("Kataragama", 6.4140, 81.3350),
        PlannerCity("Kegalle", 7.2520, 80.3470),
        PlannerCity("Kilinochchi", 9.3960, 80.3980),
        PlannerCity("Kitulgala", 6.9890, 80.4180),
        PlannerCity("Kuliyapitiya", 7.4690, 80.0410),
        PlannerCity("Kurunegala", 7.4860, 80.3630),
        PlannerCity("Mannar", 8.9780, 79.9040),
        PlannerCity("Matale", 7.4680, 80.6230),
        PlannerCity("Matara", 5.9480, 80.5350),
        PlannerCity("Mirissa", 5.9480, 80.4720),
        PlannerCity("Monaragala", 6.8730, 81.3510),
        PlannerCity("Moratuwa", 6.7740, 79.8820),
        PlannerCity("Mullaitivu", 9.2670, 80.8140),
        PlannerCity("Negombo", 7.2085, 79.8358),
        PlannerCity("Nilaveli", 8.6810, 81.2010),
        PlannerCity("Nuwara Eliya", 6.9500, 80.7890),
        PlannerCity("Panadura", 6.7130, 79.9050),
        PlannerCity("Pasikudah", 7.9250, 81.5620),
        PlannerCity("Pinnawala", 7.3000, 80.3840),
        PlannerCity("Point Pedro", 9.8160, 80.2340),
        PlannerCity("Polonnaruwa", 7.9400, 81.0190),
        PlannerCity("Pottuvil", 6.8760, 81.8310),
        PlannerCity("Puttalam", 8.0360, 79.8280),
        PlannerCity("Rambukkana", 7.3250, 80.3910),
        PlannerCity("Ratnapura", 6.6820, 80.3990),
        PlannerCity("Sigiriya", 7.9570, 80.7603),
        PlannerCity("Tangalle", 6.0240, 80.7980),
        PlannerCity("Tissamaharama", 6.2780, 81.2880),
        PlannerCity("Trincomalee", 8.5870, 81.2150),
        PlannerCity("Unawatuna", 6.0110, 80.2480),
        PlannerCity("Vavuniya", 8.7510, 80.4970),
        PlannerCity("Weligama", 5.9740, 80.4290),
        PlannerCity("Wellawaya", 6.7370, 81.1030),
        PlannerCity("Wennappuwa", 7.3410, 79.8380)
    ).sortedBy { it.name }

    // These are demo planning assumptions, not verified ticket/accommodation prices.
    const val DAILY_ALLOWANCE_LKR = 18_000L // accommodation + meals + local travel

    fun admissionEstimate(category: String): Long = when (category.lowercase()) {
        "history" -> 3_000L
        "wildlife" -> 5_000L
        "nature" -> 2_000L
        "beach" -> 500L
        else -> 1_500L
    }

    fun totalEstimate(days: Int, stops: List<PlannerStop>): Long =
        days * DAILY_ALLOWANCE_LKR + stops.sumOf { it.estimatedAdmissionLkr }

    /** Great-circle approximation, not driving distance. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 6371.0 * 2.0 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1 - a).coerceIn(0.0, 1.0)))
    }

    /**
     * Greedy nearest-neighbour order and a maximum of two attractions per day.
     * This is a prototype suggestion, not live route optimisation.
     */
    fun generate(
        startDate: LocalDate,
        numberOfDays: Int,
        startingCity: String,
        interests: Set<String>,
        budgetLkr: Long,
        places: List<PlannerPlace>,
        startingLatitude: Double? = null,
        startingLongitude: Double? = null
    ): PlannerDraft {
        require(numberOfDays in 1..10) { "Trip length must be from 1 to 10 days." }
        require(budgetLkr > 0L) { "Please enter a budget greater than zero." }
        require((startingLatitude == null) == (startingLongitude == null)) {
            "Starting coordinates must include both latitude and longitude."
        }
        val city = if (startingLatitude != null && startingLongitude != null) {
            require(startingLatitude in 5.8..10.1 && startingLongitude in 79.4..82.3) {
                "Choose a starting location within Sri Lanka."
            }
            PlannerCity(startingCity, startingLatitude, startingLongitude)
        } else {
            cities.firstOrNull { it.name.equals(startingCity, ignoreCase = true) }
                ?: error("Choose a starting location or an offline city.")
        }
        val choices = places.filter {
            (interests.isEmpty() || it.category in interests) &&
                it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
        }.distinctBy { it.id }.toMutableList()
        require(choices.isNotEmpty()) { "No mapped destinations match those interests." }
        val chosen = mutableListOf<PlannerPlace>()
        var lat = city.latitude
        var lon = city.longitude
        var remainingAdmissionBudget =
            (budgetLkr - numberOfDays * DAILY_ALLOWANCE_LKR).coerceAtLeast(0L)
        while (choices.isNotEmpty() && chosen.size < numberOfDays * 2) {
            // Only suggest additional attractions that fit the illustrative admission allowance.
            // Always include the nearest attraction when no options are affordable, then flag
            // the over-budget estimate in the UI rather than returning an empty itinerary.
            val affordable = choices.filter { admissionEstimate(it.category) <= remainingAdmissionBudget }
            val candidates = when {
                affordable.isNotEmpty() -> affordable
                chosen.isEmpty() -> choices
                else -> break
            }
            val next = candidates.minWithOrNull(
                compareBy<PlannerPlace> { distanceKm(lat, lon, it.latitude, it.longitude) }
                    .thenBy { it.id }
            ) ?: break
            choices.remove(next)
            chosen.add(next)
            remainingAdmissionBudget =
                (remainingAdmissionBudget - admissionEstimate(next.category)).coerceAtLeast(0L)
            lat = next.latitude
            lon = next.longitude
        }
        val stops = chosen.mapIndexed { index, place ->
            // Evenly allocate stops to days without more than 2 attractions per day.
            val day = index * numberOfDays / chosen.size
            PlannerStop(place.id, day, index, admissionEstimate(place.category))
        }
        return PlannerDraft(
            title = "$startingCity adventure · $numberOfDays days",
            startDate = startDate,
            numberOfDays = numberOfDays,
            startingCity = startingCity,
            startingLatitude = startingLatitude,
            startingLongitude = startingLongitude,
            interests = interests,
            budgetLkr = budgetLkr,
            stops = stops
        )
    }

    fun normalizedStops(stops: List<PlannerStop>): List<PlannerStop> =
        stops.groupBy { it.dayIndex }.toSortedMap().flatMap { (day, dayStops) ->
            dayStops.sortedWith(compareBy<PlannerStop> { it.position }.thenBy { it.destinationId })
                .mapIndexed { index, stop -> stop.copy(dayIndex = day, position = index) }
        }
}
