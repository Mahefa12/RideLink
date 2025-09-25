package com.app.ridelink.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000L // 10 seconds
    ).apply {
        setMinUpdateIntervalMillis(5000L) // 5 seconds
        setMaxUpdateDelayMillis(15000L) // 15 seconds
    }.build()

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(): Result<LocationData> {
        Log.d("RideLink", "LocationManager: getCurrentLocation() called")
        if (!hasLocationPermission()) {
            Log.e("RideLink", "LocationManager: Location permission not granted")
            return Result.failure(SecurityException("Location permission not granted"))
        }

        Log.d("RideLink", "LocationManager: Location permission granted, requesting location")
        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            Log.d("RideLink", "LocationManager: Got last known location: ${location.latitude}, ${location.longitude}")
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                timestamp = location.time
                            )
                            continuation.resume(Result.success(locationData))
                        } else {
                            Log.d("RideLink", "LocationManager: No last known location, requesting fresh location")
                            // Request fresh location if last known location is null
                            requestFreshLocation(continuation)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("RideLink", "LocationManager: Failed to get last location: ${exception.message}")
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: SecurityException) {
            Log.e("RideLink", "LocationManager: SecurityException: ${e.message}")
            Result.failure(e)
        }
    }

    private fun requestFreshLocation(
        continuation: kotlinx.coroutines.CancellableContinuation<Result<LocationData>>
    ) {
        Log.d("RideLink", "LocationManager: Requesting fresh location updates")
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            Log.d("RideLink", "LocationManager: Got fresh location: ${location.latitude}, ${location.longitude}")
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                timestamp = location.time
                            )
                            continuation.resume(Result.success(locationData))
                            fusedLocationClient.removeLocationUpdates(this)
                        } else {
                            Log.w("RideLink", "LocationManager: Fresh location result was null")
                        }
                    }

                    override fun onLocationAvailability(availability: LocationAvailability) {
                        Log.d("RideLink", "LocationManager: Location availability: ${availability.isLocationAvailable}")
                        if (!availability.isLocationAvailable) {
                            Log.e("RideLink", "LocationManager: Location not available")
                            continuation.resume(
                                Result.failure(Exception("Location not available"))
                            )
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                },
                null
            )
        } catch (e: SecurityException) {
            continuation.resume(Result.failure(e))
        }
    }

    fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                    trySend(locationData)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    close(Exception("Location not available"))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}