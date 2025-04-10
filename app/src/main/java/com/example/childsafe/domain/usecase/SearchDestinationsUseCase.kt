package com.example.childsafe.domain.usecase

import com.example.childsafe.data.model.Destination
import com.example.childsafe.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching destinations by query
 * Follows clean architecture principles by encapsulating a single business operation
 */
class SearchDestinationsUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    /**
     * Execute the use case to search for destinations
     *
     * @param query The search query string
     * @return Flow of Result containing list of matching destinations
     */
    operator fun invoke(query: String): Flow<Result<List<Destination>>> {
        return locationRepository.searchDestinations(query)
    }
}