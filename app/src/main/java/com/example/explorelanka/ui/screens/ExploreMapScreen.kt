package com.example.explorelanka.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.explorelanka.data.Destination
import com.example.explorelanka.data.DestinationData
import com.example.explorelanka.data.location.UserPosition
import com.example.explorelanka.data.weather.DestinationCoordinates
import com.example.explorelanka.viewmodel.NearbyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.util.Locale

private data class MappedPlace(
    val destination: Destination,
    val position: LatLng,
    val distanceKm: Double?
)

private fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun distanceKm(user: UserPosition, point: LatLng): Double {
    val meters = FloatArray(1)
    Location.distanceBetween(
        user.latitude, user.longitude,
        point.latitude, point.longitude,
        meters
    )
    return meters[0] / 1000.0
}

@Composable
fun ExploreMapScreen(
    focusPlaceId: String? = null,
    onBackClick: () -> Unit,
    onDestinationClick: (String) -> Unit
) {
    val context = LocalContext.current
    val factory = remember(context) { NearbyViewModel.Factory(context.applicationContext) }
    val locationViewModel: NearbyViewModel = viewModel(factory = factory)
    val uiState by locationViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.any { it }
        if (granted) locationViewModel.findMyLocation()
        else locationViewModel.onPermissionDenied()
    }

    // Use the same coordinates as the existing Step 5 weather integration.
    // Future Firestore destinations require coordinates before appearing here.
    val mapped = DestinationData.places
        .distinctBy { it.id.trim().lowercase(Locale.ROOT) }
        .mapNotNull { destination ->
            val point = DestinationCoordinates.forId(destination.id) ?: return@mapNotNull null
            val latLng = LatLng(point.latitude, point.longitude)
            MappedPlace(
                destination = destination,
                position = latLng,
                distanceKm = uiState.position?.let { distanceKm(it, latLng) }
            )
        }
    val sorted = if (uiState.position != null) mapped.sortedBy { it.distanceKm }
        else mapped.sortedBy { it.destination.name }

    val focus = mapped.find { it.destination.id == focusPlaceId }
    val startingPosition = focus?.position ?: LatLng(7.8731, 80.7718)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            startingPosition,
            if (focus == null) 7.3f else 13.0f
        )
    }
    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(mapLoaded, uiState.position) {
        val position = uiState.position
        if (mapLoaded && position != null) {
            camera.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(position.latitude, position.longitude), 10.5f
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Explore map", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Sri Lankan destinations", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (hasLocationPermission(context)) {
                        granted = true
                        locationViewModel.findMyLocation()
                    } else {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                },
                enabled = !uiState.loading
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (uiState.loading) "Locating" else "Near me")
            }
        }
        Text(
            uiState.message ?: if (uiState.position != null) {
                "Distances are straight-line estimates from your current location."
            } else {
                "Tap Near me for GPS distances, or browse the map without location permission."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        GoogleMap(
            modifier = Modifier.fillMaxWidth().weight(0.56f),
            cameraPositionState = camera,
            onMapLoaded = { mapLoaded = true },
            uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true)
        ) {
            mapped.forEach { item ->
                key(item.destination.id) {
                    Marker(
                        state = rememberMarkerState(position = item.position),
                        title = item.destination.name,
                        snippet = item.destination.district,
                        onInfoWindowClick = { onDestinationClick(item.destination.id) }
                    )
                }
            }
            uiState.position?.let { user ->
                Marker(
                    state = rememberMarkerState(
                        position = LatLng(user.latitude, user.longitude)
                    ),
                    title = "My current location"
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (uiState.position != null) "Nearest destinations" else "All mapped destinations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        if (sorted.isEmpty()) {
            Text("No destination coordinates available yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(0.44f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sorted, key = { it.destination.id }) { item ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.destination.name, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    item.distanceKm?.let {
                                        "${String.format(Locale.US, "%.1f", it)} km away (straight line)"
                                    } ?: item.destination.district,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    if (mapLoaded) camera.animate(
                                        CameraUpdateFactory.newLatLngZoom(item.position, 13f)
                                    )
                                }
                            }) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = "Show on map")
                            }
                            TextButton(onClick = { onDestinationClick(item.destination.id) }) {
                                Text("Details")
                            }
                        }
                    }
                }
            }
        }
    }
}
