package com.example.childsafe.domain.repository.navigation

import com.example.childsafe.domain.model.navigation.Route
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for route navigation operations.
 * This abstraction will make it easy to switch between local route calculation
 * and backend-provided routes in the future.
 */
interface RouteRepository {
    /**
     * Gets a route between origin and destination locations.
     * 
     * @param origin The starting location
     * @param destination The ending location
     * @param alternativesRequested Whether to request alternative routes
     * @return A flow containing Result with list of routes (primary and alternatives if requested)
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        alternativesRequested: Boolean = false
    ): Flow<Result<List<Route>>>
    
    /**
     * Clears any cached route data
     */
    suspend fun clearRouteCache()
}