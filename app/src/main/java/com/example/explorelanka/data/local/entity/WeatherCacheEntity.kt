package com.example.explorelanka.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Most recently saved conditions for one destination. */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val placeId: String,
    val temperatureCelsius: Double,
    val humidityPercent: Int?,
    val weatherCode: Int?,
    val windKmh: Double?,
    val isDay: Boolean?,
    val observedAtLocal: String?,
    val maxTemperatureCelsius: Double?,
    val minTemperatureCelsius: Double?,
    val rainChancePercent: Int?,
    val fetchedAtMillis: Long
)
