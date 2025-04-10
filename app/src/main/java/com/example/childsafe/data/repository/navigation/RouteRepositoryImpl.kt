package com.example.childsafe.data.repository.navigation

import com.example.childsafe.BuildConfig
import com.example.childsafe.data.api.navigation.GoogleMapsDirectionsService
import com.example.childsafe.data.api.navigation.RouteDto
import com.example.childsafe.domain.model.navigation.Distance
import com.example.childsafe.domain.model.navigation.Duration
import com.example.childsafe.domain.model.navigation.Route
import com.example.childsafe.domain.model.navigation.RouteBounds
import com.example.childsafe.domain.model.navigation.RouteStep
import com.example.childsafe.domain.repository.navigation.RouteRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RouteRepository that uses Google Maps Directions API.
 * This implementation provides routes calculated locally using Google's API.
 * In the future, this could be replaced or supplemented with a backend-based implementation.
 */
@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val directionsService: GoogleMapsDirectionsService
) : RouteRepository {

    override suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        alternativesRequested: Boolean
    ): Flow<Result<List<Route>>> = flow {
        try {
            val originParam = "${origin.latitude},${origin.longitude}"
            val destinationParam = "${destination.latitude},${destination.longitude}"
            
            Timber.d("Fetching route from $originParam to $destinationParam")
            
            val response = directionsService.getDirections(
                origin = originParam,
                destination = destinationParam,
                alternatives = alternativesRequested,
                apiKey = BuildConfig.MAPS_API_KEY
            )
            
            if (response.status == "OK") {
                val routes = response.routes.map { it.toDomainModel(origin, destination) }
                Timber.d("Successfully fetched ${routes.size} routes")
                emit(Result.success(routes))
            } else {
                val errorMessage = response.error_message ?: "Unknown error from Directions API: ${response.status}"
                Timber.e("Directions API error: $errorMessage")
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching directions")
            emit(Result.failure(e))
        }
    }
    
    override suspend fun clearRouteCache() {
        // No caching implemented yet, could be added in the future
    }
    
    /**
     * Maps a RouteDto from the API to our domain Route model.
     */
    private fun RouteDto.toDomainModel(origin: LatLng, destination: LatLng): Route {
        // Each route has legs (usually one unless there are waypoints)
        val firstLeg = legs.first()
        
        return Route(
            routeId = UUID.randomUUID().toString(),
            origin = origin,
            destination = destination,
            polyline = overview_polyline.points,
            distance = Distance(
                meters = firstLeg.distance.value,
                text = firstLeg.distance.text
            ),
            duration = Duration(
                seconds = firstLeg.duration.value,
                text = firstLeg.duration.text
            ),
            steps = firstLeg.steps.map { step ->
                RouteStep(
                    instruction = cleanHtmlInstructions(step.html_instructions),
                    distance = Distance(
                        meters = step.distance.value,
                        text = step.distance.text
                    ),
                    duration = Duration(
                        seconds = step.duration.value,
                        text = step.duration.text
                    ),
                    polyline = step.polyline.points,
                    maneuver = step.maneuver
                )
            },
            bounds = RouteBounds(
                northeast = LatLng(
                    bounds.northeast.lat,
                    bounds.northeast.lng
                ),
                southwest = LatLng(
                    bounds.southwest.lat,
                    bounds.southwest.lng
                )
            )
        )
    }
    
    /**
     * Cleans HTML tags from the instruction text.
     */
    private fun cleanHtmlInstructions(htmlInstructions: String): String {
        return Jsoup.parse(htmlInstructions).text()
    }
}