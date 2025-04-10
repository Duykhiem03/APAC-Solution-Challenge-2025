package com.example.childsafe.domain.usecase

import com.example.childsafe.data.model.Destination
import com.example.childsafe.data.repository.PlacesRepository
import com.example.childsafe.data.repository.PlacesRepository.PlaceType
import com.example.childsafe.domain.repository.LocationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Unified use case for searching locations, whether from Places API or backend
 */
class SearchLocationsUseCase @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val locationRepository: LocationRepository
) {
    /**
     * Search for locations matching the query string
     * Prioritizes Google Places API for local searching but can also use backend
     * 
     * @param query The search query
     * @param location Current user location for distance calculation and location bias
     * @param placeType Optional place type filter (only used with Places API)
     * @param useBackend Whether to use backend search instead of Places API
     * @return Flow emitting a Result containing a list of destinations
     */
    operator fun invoke(
        query: String,
        location: LatLng? = null,
        placeType: PlaceType = PlaceType.ANY,
        useBackend: Boolean = false
    ): Flow<Result<List<Destination>>> = flow {
        try {
            Timber.d("Searching locations with query: $query, useBackend: $useBackend")
            
            val results = if (useBackend) {
                // Use backend search via LocationRepository
                // This currently returns a Flow, so we need to collect it first
                var backendResults: Result<List<Destination>> = Result.success(emptyList())
                locationRepository.searchDestinations(query).collect { result ->
                    backendResults = result
                }
                backendResults
            } else {
                // Use Places API for local search
                val places = placesRepository.searchPlaces(query, location, placeType)
                Result.success(places)
            }
            
            emit(results)
        } catch (e: Exception) {
            Timber.e(e, "Error searching locations")
            emit(Result.failure(e))
        }
    }
}