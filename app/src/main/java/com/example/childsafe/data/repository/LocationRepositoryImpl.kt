package com.example.childsafe.data.repository

import com.example.childsafe.data.api.LocationApiService
import com.example.childsafe.data.model.Coordinates
import com.example.childsafe.data.model.Destination
import com.example.childsafe.domain.repository.LocationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LocationRepository that handles location-related data operations
 * Acts as a mediator between data sources (API) and domain layer
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationApiService: LocationApiService
) : LocationRepository {

    /**
     * Get nearby destinations based on current location
     */
    override fun getNearbyDestinations(latLng: LatLng, radius: Int): Flow<Result<List<Destination>>> = flow {
        try {
            // In the future, this will fetch from API
            // For now, return mock data
            emit(Result.success(getMockDestinations(latLng)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get destination details by ID
     */
    override fun getDestinationById(id: Int): Flow<Result<Destination>> = flow {
        try {
            // In the future, call API service: locationApiService.getDestinationById(id)
            val destination = getMockDestinations(null).find { it.id.toInt() == id }
                ?: throw Exception("Destination not found")
            emit(Result.success(destination))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search destinations by query
     */
    override fun searchDestinations(query: String): Flow<Result<List<Destination>>> = flow {
        try {
            // In the future, call API service: locationApiService.searchDestinations(query)
            val filtered = getMockDestinations(null).filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.address.contains(query, ignoreCase = true) 
            }
            emit(Result.success(filtered))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get saved destinations
     */
    override fun getSavedDestinations(userId: String): Flow<Result<List<Destination>>> = flow {
        try {
            // In the future: locationApiService.getSavedDestinations(userId)
            emit(Result.success(getMockDestinations(null)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get mock destinations for development
     * Will be replaced with real API data in production
     */
    private fun getMockDestinations(currentLocation: LatLng?): List<Destination> {
        return listOf(
            Destination(
                id = 1,
                name = "Bệnh viện Đa Lâm Thành Phố Hồ Chí Minh",
                address = "201 Nguyễn Chí Thanh, P. 12, Quận 5, TP HCM, Việt Nam",
                distance = if (currentLocation != null) "3.2 km" else "Unknown",
                coordinates = Coordinates(10.7675, 106.6685)
            ),
            Destination(
                id = 2,
                name = "Trường Đại học Sài Gòn - Cơ Sở 1",
                address = "105 Bà Huyện Thanh Quan, Phường 7, Quận 3, Việt Nam",
                distance = if (currentLocation != null) "1.5 km" else "Unknown",
                coordinates = Coordinates(10.7769, 106.6894)
            ),
            Destination(
                id = 3,
                name = "Trường THPT Nguyễn Thị Minh Khai",
                address = "275 Điện Biên Phủ, Phường 7, Quận 3, TP HCM, Việt Nam",
                distance = if (currentLocation != null) "2.4 km" else "Unknown",
                coordinates = Coordinates(10.7895, 106.6925)
            )
        )
    }
}