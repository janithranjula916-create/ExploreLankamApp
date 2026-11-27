package com.example.explorelanka.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.explorelanka.data.local.entity.WeatherCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE placeId = :placeId LIMIT 1")
    fun observe(placeId: String): Flow<WeatherCacheEntity?>

    @Upsert
    suspend fun save(weather: WeatherCacheEntity)
}
