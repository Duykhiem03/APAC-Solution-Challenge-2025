package com.example.childsafe.data.api.navigation

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Google Maps Directions API.
 */
interface GoogleMapsDirectionsService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walking", // Default to walking for child safety
        @Query("alternatives") alternatives: Boolean = false,
        @Query("key") apiKey: String
    ): DirectionsResponse
}

/**
 * Data transfer objects for the Google Maps Directions API response.
 */
data class DirectionsResponse(
    val status: String,
    val routes: List<RouteDto> = emptyList(),
    val error_message: String? = null
)

data class RouteDto(
    val summary: String,
    val legs: List<LegDto>,
    val overview_polyline: PolylineDto,
    val bounds: BoundsDto
)

data class LegDto(
    val distance: TextValueDto,
    val duration: TextValueDto,
    val start_location: LatLngDto,
    val end_location: LatLngDto,
    val steps: List<StepDto>
)

data class StepDto(
    val html_instructions: String,
    val distance: TextValueDto,
    val duration: TextValueDto,
    val start_location: LatLngDto,
    val end_location: LatLngDto,
    val polyline: PolylineDto,
    val travel_mode: String,
    val maneuver: String? = null
)

data class TextValueDto(
    val text: String,
    val value: Int
)

data class LatLngDto(
    val lat: Double,
    val lng: Double
)

data class PolylineDto(
    val points: String
)

data class BoundsDto(
    val northeast: LatLngDto,
    val southwest: LatLngDto
)