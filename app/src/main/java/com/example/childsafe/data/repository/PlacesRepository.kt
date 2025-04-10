package com.example.childsafe.data.repository

import android.content.Context
import android.location.Location
import com.example.childsafe.data.model.Destination
import com.example.childsafe.di.PlacesInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Repository for interacting with the Google Places API
 */
@Singleton
class PlacesRepository @Inject constructor(
    placesInitializer: PlacesInitializer,
    @ApplicationContext private val context: Context
) {
    // Singleton PlacesClient to avoid creating multiple instances
    private val placesClient: PlacesClient by lazy {
        Places.createClient(context)
    }
    
    // Define supported place types
    enum class PlaceType(val apiType: String) {
        ANY(PlaceTypes.GEOCODE),
        SCHOOL(PlaceTypes.SCHOOL),
        SHOPPING_MALL(PlaceTypes.SHOPPING_MALL),
        RESTAURANT(PlaceTypes.RESTAURANT),
        HOSPITAL(PlaceTypes.HOSPITAL),
        PARK(PlaceTypes.PARK)
    }
    
    init {
        // Initialize Places SDK when repository is created
        placesInitializer.initialize()
    }

    /**
     * Search for places by query string and optional place type
     * @param query The search query
     * @param location The current user location to bias results toward
     * @param placeType Optional place type to filter results (defaults to ANY)
     * @return List of destinations matching the query and type
     */
    suspend fun searchPlaces(
        query: String, 
        location: LatLng?,
        placeType: PlaceType = PlaceType.ANY
    ): List<Destination> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) return@withContext emptyList<Destination>()
                
                // Create session token for grouping related requests
                val token = AutocompleteSessionToken.newInstance()
                
                // Create location bias if we have current location
                val locationBias = location?.let { userLocation ->
                    // Create a bias area around the user's location (approximately 50km radius)
                    val latWindow = 0.5 // ~50km
                    val lngWindow = 0.5 // ~50km
                    RectangularBounds.newInstance(
                        LatLng(userLocation.latitude - latWindow, userLocation.longitude - lngWindow),
                        LatLng(userLocation.latitude + latWindow, userLocation.longitude + lngWindow)
                    )
                }
                
                // Build the autocomplete request with place type filter if specified
                val requestBuilder = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(token)
                    .setQuery(query)
                
                // Set type filter based on place type
                if (placeType != PlaceType.ANY) {
                    // When specifying a place type other than ANY, we use that specific type
                    requestBuilder.setTypesFilter(listOf(placeType.apiType))
                    Timber.d("Searching for place type: ${placeType.name}")
                } else {
                    // For ANY, use GEOCODE which is the most inclusive type
                    requestBuilder.setTypesFilter(listOf(PlaceTypes.GEOCODE))
                }
                
                // Add location bias if available
                locationBias?.let { bounds ->
                    requestBuilder.setLocationBias(bounds)
                }
                
                val request = requestBuilder.build()
                
                // Execute the request
                val predictions = suspendCancellableCoroutine { continuation ->
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            continuation.resume(response.autocompletePredictions)
                        }
                        .addOnFailureListener { exception ->
                            Timber.e(exception, "Place autocomplete prediction failed")
                            continuation.resume(emptyList())
                        }
                }
                
                // Convert predictions to destinations and fetch distance if possible
                predictions.mapIndexed { index, prediction ->
                    predictionToDestination(
                        prediction = prediction,
                        userLocation = location,
                        token = token,
                        index = index + 1
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching places")
                emptyList()
            }
        }
    }
    
    /**
     * Convert an autocomplete prediction to a destination
     */
    private suspend fun predictionToDestination(
        prediction: AutocompletePrediction,
        userLocation: LatLng?,
        token: AutocompleteSessionToken,
        index: Int
    ): Destination {
        // For each prediction, we need to fetch more details
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        
        // Fixed: Removed the token parameter as it's not supported in this method
        val fetchRequest = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
        
        // Try to get place details
        return try {
            val placeResult = suspendCancellableCoroutine<Place?> { continuation ->
                placesClient.fetchPlace(fetchRequest)
                    .addOnSuccessListener { response ->
                        continuation.resume(response.place)
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(exception, "Place details fetch failed")
                        continuation.resume(null)
                    }
            }
            
            // Calculate distance if we have both user location and place location
            val distance = if (userLocation != null && placeResult?.latLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    placeResult.latLng!!.latitude, placeResult.latLng!!.longitude,
                    results
                )
                // Format distance in km
                String.format("%.1f km", results[0] / 1000)
            } else {
                "Unknown"
            }
            
            Destination(
                id = index.toLong(),
                name = placeResult?.name ?: prediction.getPrimaryText(null).toString(),
                address = placeResult?.address ?: prediction.getSecondaryText(null).toString(),
                distance = distance,
                placeId = prediction.placeId,
                latLng = placeResult?.latLng
            )
        } catch (e: Exception) {
            Timber.e(e, "Error converting prediction to destination")
            // Fallback if we can't get details
            Destination(
                id = index.toLong(),
                name = prediction.getPrimaryText(null).toString(),
                address = prediction.getSecondaryText(null).toString(),
                distance = "Unknown",
                placeId = prediction.placeId,
                latLng = null
            )
        }
    }
    
    /**
     * Properly shutdown the Places client when repository is destroyed
     * This is necessary to avoid the "Previous channel was not shutdown properly" error
     */
    fun shutdown() {
        try {
            // The Places SDK doesn't expose a direct shutdown method, so we use reflection
            // to access the underlying shutdown method of the ManagedChannel
            val placesClientClass = placesClient.javaClass
            val shutdownMethod = findShutdownMethod(placesClientClass)
            
            shutdownMethod?.let {
                it.invoke(placesClient)
                Timber.d("PlacesClient shutdown successfully")
            } ?: Timber.e("Could not find shutdown method for PlacesClient")
            
        } catch (e: Exception) {
            Timber.e(e, "Error shutting down PlacesClient")
        }
    }
    
    /**
     * Find the shutdown method in the PlacesClient or its parent classes using reflection
     */
    private fun findShutdownMethod(clazz: Class<*>): Method? {
        // Try to find the shutdown method in this class
        try {
            return clazz.getDeclaredMethod("shutdown").apply { isAccessible = true }
        } catch (e: NoSuchMethodException) {
            // Method not found in this class
        }
        
        // Try to find the shutdownNow method in this class
        try {
            return clazz.getDeclaredMethod("shutdownNow").apply { isAccessible = true }
        } catch (e: NoSuchMethodException) {
            // Method not found in this class
        }
        
        // Look in parent class
        val superClass = clazz.superclass
        return if (superClass != null && superClass != Any::class.java) {
            findShutdownMethod(superClass)
        } else {
            null
        }
    }
    
    /**
     * Called when repository is destroyed
     */
    fun onDestroy() {
        shutdown()
    }
}