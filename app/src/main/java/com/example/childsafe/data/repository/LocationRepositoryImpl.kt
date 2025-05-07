package com.example.childsafe.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.example.childsafe.data.api.LocationApiService
import com.example.childsafe.data.model.Coordinates
import com.example.childsafe.data.model.Destination
import com.example.childsafe.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

/**
 * Implementation of LocationRepository that handles location-related data operations
 * Acts as a mediator between data sources (API) and domain layer
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationApiService: LocationApiService,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val context: Context
) : LocationRepository {

    // Store recent locations to calculate speed and detect movement
    private val recentLocations = mutableListOf<LocationWithTimestamp>()
    
    // For motion detection
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private var movementDetected = false
    
    // SensorEventListener for motion detection
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    // Check for significant motion
                    val magnitude = sqrt(
                        accelerometerReading[0] * accelerometerReading[0] +
                        accelerometerReading[1] * accelerometerReading[1] +
                        accelerometerReading[2] * accelerometerReading[2]
                    )
                    // If magnitude differs significantly from gravity, device is likely moving
                    movementDetected = kotlin.math.abs(magnitude - 9.8f) > MOVEMENT_THRESHOLD
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not needed for this implementation
        }
    }
    
    init {
        // Register sensor listeners for motion detection
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
            sensorManager.registerListener(
                sensorEventListener, 
                accelerometer, 
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { magnetometer ->
            sensorManager.registerListener(
                sensorEventListener, 
                magnetometer, 
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    /**
     * Get nearby destinations based on current location
     */
    override fun getNearbyDestinations(latLng: LatLng, radius: Int): Flow<Result<List<Destination>>> = flow {
        try {
            // In the future, this will fetch from API
            // For now, return mock data
            emit(Result.success(getMockDestinations(latLng)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get destination details by ID
     */
    override fun getDestinationById(id: Int): Flow<Result<Destination>> = flow {
        try {
            // In the future, call API service: locationApiService.getDestinationById(id)
            val destination = getMockDestinations(null).find { it.id.toInt() == id }
                ?: throw Exception("Destination not found")
            emit(Result.success(destination))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search destinations by query
     */
    override fun searchDestinations(query: String): Flow<Result<List<Destination>>> = flow {
        try {
            // In the future, call API service: locationApiService.searchDestinations(query)
            val filtered = getMockDestinations(null).filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.address.contains(query, ignoreCase = true) 
            }
            emit(Result.success(filtered))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get saved destinations
     */
    override fun getSavedDestinations(userId: String): Flow<Result<List<Destination>>> = flow {
        try {
            // In the future: locationApiService.getSavedDestinations(userId)
            emit(Result.success(getMockDestinations(null)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Gets the current device location
     * Uses FusedLocationProviderClient for accurate and power-efficient location
     */
    override suspend fun getCurrentLocation(): Location = withTimeout(LOCATION_TIMEOUT) {
        try {
            // Explicitly check permissions before attempting to access location
            if (!checkLocationPermission()) {
                throw SecurityException("Location permission is required but not granted")
            }
            
            try {
                // First try to get the last known location (fast)
                val lastLocation = fusedLocationClient.lastLocation.await()
                
                // If recent location available and not stale, use it
                if (lastLocation != null && 
                    System.currentTimeMillis() - lastLocation.time < MAX_LOCATION_AGE_MS) {
                    
                    // Store in recent locations for movement calculation
                    addLocationToHistory(lastLocation)
                    return@withTimeout lastLocation
                }
                
                // Otherwise request a fresh location
                return@withTimeout suspendCancellableCoroutine { continuation ->
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        FAST_LOCATION_INTERVAL_MS
                    ).build()
                    
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            fusedLocationClient.removeLocationUpdates(this)
                            val location = result.lastLocation
                            if (location != null) {
                                addLocationToHistory(location)
                                continuation.resume(location)
                            } else {
                                continuation.resumeWithException(
                                    Exception("Location unavailable")
                                )
                            }
                        }
                    }
                    
                    try {
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                    } catch (se: SecurityException) {
                        // Handle security exception from requestLocationUpdates
                        continuation.resumeWithException(
                            SecurityException("Location permission denied: ${se.message}")
                        )
                    }
                    
                    continuation.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }
                }
            } catch (se: SecurityException) {
                // Specifically catch and rethrow SecurityException
                throw SecurityException("Location permission denied: ${se.message}")
            }
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> throw e
                else -> throw Exception("Failed to get location: ${e.message}")
            }
        }
    }
    
    /**
     * Check if location permissions are granted
     * @return true if either fine or coarse location permission is granted
     */
    private fun checkLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED || 
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Extension function to check if permission is granted
     */
    private fun String.hasPermission(context: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            this
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Determines if the device is currently in motion
     * Uses both accelerometer data and location changes
     */
    override suspend fun isDeviceMoving(): Boolean {
        // First check accelerometer-based motion detection
        if (movementDetected) {
            return true
        }
        
        // Then check location-based movement
        if (recentLocations.size < 2) {
            return false
        }
        
        val timeWindow = System.currentTimeMillis() - MOVEMENT_TIME_WINDOW_MS
        val recentEnoughLocations = recentLocations.filter { it.timestamp >= timeWindow }
        
        if (recentEnoughLocations.size < 2) {
            return false
        }
        
        // Calculate distance between first and last recent locations
        val firstLocation = recentEnoughLocations.first().location
        val lastLocation = recentEnoughLocations.last().location
        
        val distance = firstLocation.distanceTo(lastLocation)
        val timeSpan = lastLocation.time - firstLocation.time
        
        // Consider moving if distance is significant over this time span
        return distance > MINIMUM_DISTANCE_FOR_MOVEMENT && 
               timeSpan >= MINIMUM_TIME_FOR_MOVEMENT
    }

    /**
     * Gets the current speed of the device in meters/second
     * Uses recent location updates to calculate average speed
     */
    override suspend fun getCurrentSpeed(): Float {
        // If we have no or only one location, can't calculate speed
        if (recentLocations.size < 2) {
            return 0.0f
        }
        
        // Filter to recent locations only
        val timeWindow = System.currentTimeMillis() - SPEED_CALCULATION_WINDOW_MS
        val recentEnoughLocations = recentLocations.filter { it.timestamp >= timeWindow }
        
        if (recentEnoughLocations.size < 2) {
            return 0.0f
        }
        
        // Use the speed value from the most recent location if available
        val mostRecent = recentEnoughLocations.last().location
        if (mostRecent.hasSpeed()) {
            return mostRecent.speed
        }
        
        // Calculate average speed from multiple location points
        var totalDistance = 0.0f
        for (i in 1 until recentEnoughLocations.size) {
            val prev = recentEnoughLocations[i-1].location
            val current = recentEnoughLocations[i].location
            totalDistance += prev.distanceTo(current)
        }
        
        val timeSpan = (recentEnoughLocations.last().timestamp - 
                       recentEnoughLocations.first().timestamp) / 1000.0 // convert to seconds
                       
        return if (timeSpan > 0) totalDistance / timeSpan.toFloat() else 0.0f
    }
    
    /**
     * Adds a location to the history for speed and movement calculations
     */
    private fun addLocationToHistory(location: Location) {
        synchronized(recentLocations) {
            recentLocations.add(LocationWithTimestamp(location, System.currentTimeMillis()))
            // Prune old locations
            val cutoffTime = System.currentTimeMillis() - MAX_LOCATION_HISTORY_MS
            while (recentLocations.size > MAX_LOCATIONS_HISTORY_SIZE || 
                   (recentLocations.isNotEmpty() && recentLocations.first().timestamp < cutoffTime)) {
                recentLocations.removeAt(0)
            }
        }
    }
    
    /**
     * Get mock destinations for development
     * Will be replaced with real API data in production
     */
    private fun getMockDestinations(currentLocation: LatLng?): List<Destination> {
        return listOf(
            Destination(
                id = 1,
                name = "Bệnh viện Đa Lâm Thành Phố Hồ Chí Minh",
                address = "201 Nguyễn Chí Thanh, P. 12, Quận 5, TP HCM, Việt Nam",
                distance = if (currentLocation != null) "3.2 km" else "Unknown",
                coordinates = Coordinates(10.7675, 106.6685)
            ),
            Destination(
                id = 2,
                name = "Trường Đại học Sài Gòn - Cơ Sở 1",
                address = "105 Bà Huyện Thanh Quan, Phường 7, Quận 3, Việt Nam",
                distance = if (currentLocation != null) "1.5 km" else "Unknown",
                coordinates = Coordinates(10.7769, 106.6894)
            ),
            Destination(
                id = 3,
                name = "Trường THPT Nguyễn Thị Minh Khai",
                address = "275 Điện Biên Phủ, Phường 7, Quận 3, TP HCM, Việt Nam",
                distance = if (currentLocation != null) "2.4 km" else "Unknown",
                coordinates = Coordinates(10.7895, 106.6925)
            )
        )
    }
    
    /**
     * Helper class to track location with its timestamp
     */
    private data class LocationWithTimestamp(
        val location: Location,
        val timestamp: Long
    )

    companion object {
        // Constants for location and movement detection
        private const val LOCATION_TIMEOUT = 15000L // 15 seconds timeout for getting location
        private const val MAX_LOCATION_AGE_MS = 30000L // 30 seconds max age for cached location
        private const val FAST_LOCATION_INTERVAL_MS = 5000L // 5 seconds for fresh location
        
        private const val MAX_LOCATIONS_HISTORY_SIZE = 20 // Maximum number of locations to keep
        private const val MAX_LOCATION_HISTORY_MS = 120000L // 2 minutes of location history
        
        private const val MOVEMENT_THRESHOLD = 1.0f // m/s² above gravity to detect movement
        private const val MOVEMENT_TIME_WINDOW_MS = 60000L // 1 minute window for movement detection
        private const val MINIMUM_DISTANCE_FOR_MOVEMENT = 10.0f // 10 meters minimum to be considered moving
        private const val MINIMUM_TIME_FOR_MOVEMENT = 10000L // 10 seconds minimum time window
        
        private const val SPEED_CALCULATION_WINDOW_MS = 30000L // 30 seconds for speed calculation
    }
}