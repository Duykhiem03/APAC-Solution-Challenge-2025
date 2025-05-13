package com.example.childsafe.domain.repository

import android.location.Location
import com.example.childsafe.data.model.Destination
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location-related functionality
 * Provides methods for accessing device location data
 */
interface LocationRepository {
    /**
     * Get nearby destinations based on current location
     */
    fun getNearbyDestinations(latLng: LatLng, radius: Int = 5000): Flow<Result<List<Destination>>>
    
    /**
     * Get destination details by ID
     */
    fun getDestinationById(id: Int): Flow<Result<Destination>>
    
    /**
     * Search destinations by query
     */
    fun searchDestinations(query: String): Flow<Result<List<Destination>>>
    
    /**
     * Get saved/favorite destinations for a user
     */
    fun getSavedDestinations(userId: String): Flow<Result<List<Destination>>>

    /**
     * Gets the current device location
     * @return Current location object
     * @throws SecurityException if location permissions aren't granted
     * @throws Exception if location can't be determined
     */
    suspend fun getCurrentLocation(): Location

    /**
     * Determines if the device is currently in motion
     * Uses accelerometer data and recent location changes
     * @return true if the device is moving, false otherwise
     */
    suspend fun isDeviceMoving(): Boolean

    /**
     * Gets the current speed of the device in meters/second
     * @return Current speed (0.0f if device is stationary)
     */
    suspend fun getCurrentSpeed(): Float
}