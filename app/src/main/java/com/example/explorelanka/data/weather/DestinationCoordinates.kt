
package com.example.explorelanka.data.weather

import java.util.Locale

data class PlaceCoordinates(
    val latitude: Double,
    val longitude: Double
)

object DestinationCoordinates {

    private val knownPlaces = mapOf(
        "sigiriya" to PlaceCoordinates(7.9570, 80.7603),

        "nine_arch_bridge" to PlaceCoordinates(6.8768, 81.0616),

        "galle_fort" to PlaceCoordinates(6.0309, 80.2168),

        "mirissa_beach" to PlaceCoordinates(5.9483, 80.4716),

        "peradeniya_gardens" to PlaceCoordinates(7.2715, 80.5960),

        "yala_national_park" to PlaceCoordinates(6.3701, 81.5185),

        "temple_of_tooth" to PlaceCoordinates(7.2936, 80.6413),

        "unawatuna_beach" to PlaceCoordinates(6.0106, 80.2484)
    )

    fun forId(placeId: String): PlaceCoordinates? {
        val normalizedId = placeId.trim().lowercase(Locale.ROOT)
        return knownPlaces[normalizedId]
    }
}
