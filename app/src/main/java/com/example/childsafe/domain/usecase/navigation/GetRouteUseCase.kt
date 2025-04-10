package com.example.childsafe.domain.usecase.navigation

import com.example.childsafe.domain.model.navigation.Route
import com.example.childsafe.domain.repository.navigation.RouteRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving navigation routes between two locations.
 */
class GetRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {
    /**
     * Get a route between origin and destination.
     *
     * @param origin The starting location
     * @param destination The ending location
     * @param alternativesRequested Whether to request alternative routes
     * @return Flow of Result containing list of routes
     */
    suspend operator fun invoke(
        origin: LatLng,
        destination: LatLng,
        alternativesRequested: Boolean = false
    ): Flow<Result<List<Route>>> {
        return routeRepository.getRoute(
            origin = origin,
            destination = destination,
            alternativesRequested = alternativesRequested
        )
    }
}