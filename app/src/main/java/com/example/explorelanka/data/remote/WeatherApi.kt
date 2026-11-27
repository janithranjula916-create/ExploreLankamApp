package com.example.explorelanka.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo's Forecast endpoint. All network calls are suspend/non-blocking. */
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day",
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}

data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather? = null,
    @SerializedName("daily") val daily: DailyWeather? = null
)

data class CurrentWeather(
    @SerializedName("time") val time: String? = null,
    @SerializedName("temperature_2m") val temperatureCelsius: Double? = null,
    @SerializedName("relative_humidity_2m") val humidityPercent: Int? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("wind_speed_10m") val windKmh: Double? = null,
    @SerializedName("is_day") val isDay: Int? = null
)

data class DailyWeather(
    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double?>? = null,
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double?>? = null,
    @SerializedName("precipitation_probability_max") val maxRainChances: List<Int?>? = null
)

object WeatherApiProvider {
    val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
