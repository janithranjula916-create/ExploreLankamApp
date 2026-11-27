package com.example.explorelanka.data.repository

import com.example.explorelanka.data.local.dao.WeatherDao
import com.example.explorelanka.data.local.entity.WeatherCacheEntity
import com.example.explorelanka.data.remote.WeatherApi
import kotlinx.coroutines.flow.Flow

/** Room is the UI's data source; the API updates the Room cache. */
class WeatherRepository(
    private val weatherDao: WeatherDao,
    private val weatherApi: WeatherApi
) {
    fun observe(placeId: String): Flow<WeatherCacheEntity?> = weatherDao.observe(placeId)

    suspend fun refresh(placeId: String, latitude: Double, longitude: Double) {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0) {
            "Invalid destination coordinates"
        }
        val response = weatherApi.getForecast(latitude, longitude)
        val current = response.current ?: error("Weather service returned no current conditions")
        val temperature = current.temperatureCelsius
            ?: error("Weather service returned no temperature")
        require(temperature.isFinite()) { "Invalid temperature received" }

        val daily = response.daily
        weatherDao.save(
            WeatherCacheEntity(
                placeId = placeId,
                temperatureCelsius = temperature,
                humidityPercent = current.humidityPercent,
                weatherCode = current.weatherCode,
                windKmh = current.windKmh,
                isDay = current.isDay?.let { it == 1 },
                observedAtLocal = current.time,
                maxTemperatureCelsius = daily?.maxTemperatures?.firstOrNull(),
                minTemperatureCelsius = daily?.minTemperatures?.firstOrNull(),
                rainChancePercent = daily?.maxRainChances?.firstOrNull(),
                fetchedAtMillis = System.currentTimeMillis()
            )
        )
    }
}
