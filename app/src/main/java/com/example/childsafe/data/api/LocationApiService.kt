package com.example.childsafe.data.api

import com.example.childsafe.data.model.Destination
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API service interface for location-based operations
 * This will connect with backend API using Node.js and Python for AI
 */
interface LocationApiService {
    /**
     * Get nearby destinations based on current location
     */
    @GET("destinations/nearby")
    suspend fun getNearbyDestinations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 5000
    ): Response<List<Destination>>
    
    /**
     * Get destination details by ID
     */
    @GET("destinations/{id}")
    suspend fun getDestinationById(@Path("id") id: Int): Response<Destination>
    
    /**
     * Search destinations by query
     */
    @GET("destinations/search")
    suspend fun searchDestinations(@Query("query") query: String): Response<List<Destination>>
    
    /**
     * Get saved/favorite destinations for a user
     */
    @GET("users/{userId}/saved-destinations")
    suspend fun getSavedDestinations(@Path("userId") userId: String): Response<List<Destination>>
}