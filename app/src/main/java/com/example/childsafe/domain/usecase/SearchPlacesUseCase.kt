package com.example.childsafe.domain.usecase

import com.example.childsafe.data.model.Destination
import com.example.childsafe.data.repository.PlacesRepository
import com.example.childsafe.data.repository.PlacesRepository.PlaceType
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for searching places using Google Places API
 */
class SearchPlacesUseCase @Inject constructor(
    private val placesRepository: PlacesRepository
) {
    /**
     * Search for places matching the query string
     * @param query The search query
     * @param location Current user location for distance calculation and location bias
     * @param placeType Optional place type filter
     * @return Flow emitting a Result containing a list of destinations or an exception
     */
    operator fun invoke(
        query: String, 
        location: LatLng?, 
        placeType: PlaceType = PlaceType.ANY
    ): Flow<Result<List<Destination>>> = flow {
        try {
            Timber.d("Searching places with query: $query, type: ${placeType.name}")
            val places = placesRepository.searchPlaces(query, location, placeType)
            Timber.d("Places search returned ${places.size} results")
            emit(Result.success(places))
        } catch (e: Exception) {
            Timber.e(e, "Error searching places")
            emit(Result.failure(e))
        }
    }
}