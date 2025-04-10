package com.example.childsafe.domain.repository

import com.example.childsafe.data.model.Destination
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining location-related data operations
 * Provides a clean boundary between data and domain layers
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
}