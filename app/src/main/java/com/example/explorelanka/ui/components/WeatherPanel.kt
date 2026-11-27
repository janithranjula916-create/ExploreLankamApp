package com.example.explorelanka.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.explorelanka.data.local.ExploreLankaDatabase
import com.example.explorelanka.data.local.entity.WeatherCacheEntity
import com.example.explorelanka.data.remote.WeatherApiProvider
import com.example.explorelanka.data.repository.WeatherRepository
import com.example.explorelanka.data.weather.DestinationCoordinates
import com.example.explorelanka.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class WeatherAppearance(val description: String, val icon: ImageVector)

private fun appearanceFor(code: Int?): WeatherAppearance = when (code) {
    0 -> WeatherAppearance("Clear sky", Icons.Outlined.WbSunny)
    1, 2 -> WeatherAppearance("Partly cloudy", Icons.Outlined.Cloud)
    3 -> WeatherAppearance("Overcast", Icons.Outlined.Cloud)
    45, 48 -> WeatherAppearance("Foggy", Icons.Outlined.Cloud)
    in 51..67, in 80..82 -> WeatherAppearance("Rain or drizzle", Icons.Outlined.Grain)
    in 71..77, in 85..86 -> WeatherAppearance("Snow", Icons.Outlined.Cloud)
    in 95..99 -> WeatherAppearance("Thunderstorms", Icons.Outlined.Thunderstorm)
    else -> WeatherAppearance("Weather conditions", Icons.Outlined.Cloud)
}

private fun formatC(value: Double?): String =
    value?.let { String.format(Locale.getDefault(), "%.1f °C", it) } ?: "Not available"

private fun savedAt(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))

@Composable
fun WeatherPanel(destinationId: String) {
    val coordinates = remember(destinationId) { DestinationCoordinates.forId(destinationId) }
    if (coordinates == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Weather is not configured for this destination yet.")
            }
        }
        return
    }

    val context = LocalContext.current.applicationContext
    val db = remember(context) { ExploreLankaDatabase.getInstance(context) }
    val repository = remember(db) { WeatherRepository(db.weatherDao(), WeatherApiProvider.api) }
    val weatherViewModel: WeatherViewModel = viewModel(
        key = "weather-$destinationId",
        factory = WeatherViewModel.factory(repository, destinationId, coordinates)
    )
    val weather by weatherViewModel.weather.collectAsState()

    // Runs on entry to the destination page, not on every recomposition.
    LaunchedEffect(destinationId) { weatherViewModel.refresh() }

    val uriHandler = LocalUriHandler.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Destination weather",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Current conditions and today's forecast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { weatherViewModel.refresh() },
                    enabled = !weatherViewModel.isLoading
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh weather")
                }
            }

            if (weatherViewModel.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (weatherViewModel.errorMessage != null) {
                Text(
                    weatherViewModel.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                if (weather != null) {
                    Text(
                        "Showing the last weather saved on this device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val saved: WeatherCacheEntity? = weather
            if (saved != null) {
                val conditions = appearanceFor(saved.weatherCode)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = conditions.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            formatC(saved.temperatureCelsius),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(conditions.description)
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.WaterDrop, contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Humidity", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(saved.humidityPercent?.let { "$it%" } ?: "Not available")
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Air, contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Wind", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(saved.windKmh?.let {
                            String.format(Locale.getDefault(), "%.1f km/h", it)
                        } ?: "Not available")
                    }
                }
                Text(
                    "Today: ${formatC(saved.minTemperatureCelsius)} – ${formatC(saved.maxTemperatureCelsius)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                saved.rainChancePercent?.let {
                    Text("Maximum rain probability today: $it%")
                }
                Text(
                    "Saved: ${savedAt(saved.fetchedAtMillis)} (device time)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!weatherViewModel.isLoading && weatherViewModel.errorMessage == null) {
                Text("No saved weather available yet. Use Refresh to try again.")
            } else if (!weatherViewModel.isLoading) {
                Text("No offline weather is available for this destination yet.")
            }

            TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/") }) {
                Text("Weather data: Open-Meteo")
            }
        }
    }
}
