package com.example.childsafe.domain.usecase

import com.example.childsafe.data.model.Destination
import com.example.childsafe.domain.repository.LocationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting nearby destinations based on current location
 * Follows clean architecture principles by encapsulating a single business operation
 */
class GetNearbyDestinationsUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    /**
     * Execute the use case to get nearby destinations
     *
     * @param latLng The current location coordinates
     * @param radius The radius in meters to search for destinations
     * @return Flow of Result containing list of nearby destinations
     */
    operator fun invoke(latLng: LatLng, radius: Int = 5000): Flow<Result<List<Destination>>> {
        return locationRepository.getNearbyDestinations(latLng, radius)
    }
}