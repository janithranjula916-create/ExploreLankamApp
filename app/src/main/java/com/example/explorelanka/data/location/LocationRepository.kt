package com.example.explorelanka.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/** Retrieves one foreground location fix only when the user requests it. */
class LocationRepository(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentPosition(): UserPosition? {
        if (!hasLocationPermission()) throw SecurityException("Location permission is not granted")

        val tokenSource = CancellationTokenSource()
        return try {
            val location: Location? = client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).await()
            location?.let { UserPosition(it.latitude, it.longitude) }
        } finally {
            tokenSource.cancel()
        }
    }
}

data class UserPosition(val latitude: Double, val longitude: Double)
