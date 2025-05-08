package com.example.childsafe.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.R
import com.example.childsafe.data.model.Destination
import com.example.childsafe.data.repository.PlacesRepository.PlaceType
import com.example.childsafe.domain.usecase.GetNearbyDestinationsUseCase
import com.example.childsafe.domain.usecase.SearchDestinationsUseCase
import com.example.childsafe.domain.usecase.SearchPlacesUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val application: Application,
    private val getNearbyDestinationsUseCase: GetNearbyDestinationsUseCase? = null,
    private val searchDestinationsUseCase: SearchDestinationsUseCase? = null,
    private val searchPlacesUseCase: SearchPlacesUseCase? = null
) : AndroidViewModel(application) {

    // Initialize FusedLocationProviderClient in init block rather than lazily
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Geocoder for reverse geocoding
    private val geocoder = Geocoder(application, Locale.getDefault())

    // State flows for UI state management
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    // Flag to indicate if this is a real location or the default
    private val _isRealLocation = MutableStateFlow(false)
    val isRealLocation: StateFlow<Boolean> = _isRealLocation.asStateFlow()

    // Flag to force UI update when location changes
    private val _locationUpdateTrigger = MutableStateFlow(0)
    val locationUpdateTrigger: StateFlow<Int> = _locationUpdateTrigger.asStateFlow()

    private val _currentCityName = MutableStateFlow(application.getString(R.string.default_city))
    val currentCityName: StateFlow<String> = _currentCityName.asStateFlow()

    private val _nearbyDestinations = MutableStateFlow<List<Destination>>(emptyList())
    val nearbyDestinations: StateFlow<List<Destination>> = _nearbyDestinations.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPlaceType = MutableStateFlow(PlaceType.ANY)
    val selectedPlaceType: StateFlow<PlaceType> = _selectedPlaceType.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Destination>>(emptyList())
    val searchResults: StateFlow<List<Destination>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Default location set to Ho Chi Minh City
    private val defaultLocation = LatLng(10.8231, 106.6297)
    private val defaultCityName = application.getString(R.string.default_city)
    
    // Flag to track if we're using default location
    private var isUsingDefaultLocation = true
    
    // Flag to track if we're actively trying to get user location
    private var isRequestingLocation = false
    
    // Job for handling search debounce
    private var searchJob: Job? = null

    // Location callback for continuous updates
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                Timber.d("LocationViewModel: Real location received in callback: ${location.latitude}, ${location.longitude}")
                updateLocationState(location)
                isUsingDefaultLocation = false
                isRequestingLocation = false
                _isRealLocation.value = true
                
                // Increment trigger to force UI update
                _locationUpdateTrigger.value += 1
            }
        }
    }

    // Initialize with last known location
    init {
        Timber.d("LocationViewModel initialized")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    }

    // Update location permission status
    fun updateLocationPermissionStatus(isGranted: Boolean) {
        Timber.d("LocationViewModel: Permission status updated to $isGranted, current state is ${_locationPermissionGranted.value}")
        
        // Set the permission state immediately regardless of previous value
        // We need to ensure UI components reflect the current permission state
        _locationPermissionGranted.value = isGranted
        
        if (isGranted) {
            Timber.d("LocationViewModel: Permission is now granted, performing location update")
            
            // If permission was just granted, reset location state to ensure we get fresh data
            if (isUsingDefaultLocation) {
                Timber.d("LocationViewModel: Was using default location, resetting state")
                // Allow new location fetch to happen right away
                _currentLocation.value = null
                isUsingDefaultLocation = false
                _isRealLocation.value = false
                
                // Increment to trigger UI update and show loading state
                _locationUpdateTrigger.value += 1
            }
            
            // Stop any existing location updates before starting fresh
            stopLocationUpdates()
            
            // Make multiple attempts to get the location
            getLocationWithRetry()
            
            // When permission is granted, also fetch nearby destinations
            updateNearbyDestinations()
        } else {
            Timber.d("LocationViewModel: Permission is denied, using default location")
            // If permission denied, use default location
            _currentLocation.value = defaultLocation
            _currentCityName.value = defaultCityName
            isUsingDefaultLocation = true
            _isRealLocation.value = false
            
            // Stop location updates if permission was revoked
            stopLocationUpdates()
            
            // Increment to trigger UI update
            _locationUpdateTrigger.value += 1
        }
    }

    // Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        } else {
            // Cancel previous search job if it's still running
            searchJob?.cancel()
            
            // Start a new search job with debounce
            searchJob = viewModelScope.launch {
                delay(300) // Debounce for 300ms
                searchPlaces(query)
            }
        }
    }
    
    // Update selected place type
    fun updateSelectedPlaceType(placeType: PlaceType) {
        if (_selectedPlaceType.value != placeType) {
            _selectedPlaceType.value = placeType
            Timber.d("Selected place type updated to: ${placeType.name}")
            
            // If we have an active search query, re-run the search with the new place type
            if (_searchQuery.value.isNotBlank()) {
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    searchPlaces(_searchQuery.value)
                }
            }
        }
    }

    // Search places using Google Places API
    private fun searchPlaces(query: String) {
        viewModelScope.launch {
            // Use the new SearchPlacesUseCase if available
            searchPlacesUseCase?.let { useCase ->
                _isLoading.value = true
                useCase(query, _currentLocation.value, _selectedPlaceType.value)
                    .catch { exception ->
                        _error.value = exception.localizedMessage
                        _isLoading.value = false
                        Timber.e(exception, "Error searching places")
                    }
                    .collect { result ->
                        _isLoading.value = false
                        result.fold(
                            onSuccess = { destinations ->
                                // Sort destinations by distance before exposing to UI
                                val sortedDestinations = sortDestinationsByDistance(destinations)
                                _searchResults.value = sortedDestinations
                                Timber.d("Places search for ${_selectedPlaceType.value.name} returned ${destinations.size} results (sorted by distance)")
                            },
                            onFailure = { exception ->
                                _error.value = exception.localizedMessage
                                Timber.e(exception, "Error processing places search results")
                            }
                        )
                    }
            } ?: run {
                // Fall back to old search if the new use case is not available
                searchDestinations(query)
            }
        }
    }

    // Legacy search method using local database (keep for backward compatibility)
    private fun searchDestinations(query: String) {
        viewModelScope.launch {
            searchDestinationsUseCase?.let { useCase ->
                _isLoading.value = true
                useCase(query)
                    .catch { exception ->
                        _error.value = exception.localizedMessage
                        _isLoading.value = false
                        Timber.e(exception, "Error searching destinations")
                    }
                    .collect { result ->
                        _isLoading.value = false
                        result.fold(
                            onSuccess = { destinations ->
                                // Sort destinations by distance before exposing to UI
                                val sortedDestinations = sortDestinationsByDistance(destinations)
                                _searchResults.value = sortedDestinations
                            },
                            onFailure = { exception ->
                                _error.value = exception.localizedMessage
                                Timber.e(exception, "Error processing search results")
                            }
                        )
                    }
            } ?: run {
                // Fallback when useCase is null (temporary until dependency injection is fully implemented)
                _searchResults.value = emptyList()
                Timber.w("SearchDestinationsUseCase not injected yet")
            }
        }
    }

    // Update nearby destinations based on current location
    private fun updateNearbyDestinations() {
        viewModelScope.launch {
            _currentLocation.value?.let { location ->
                getNearbyDestinationsUseCase?.let { useCase ->
                    _isLoading.value = true
                    useCase(location)
                        .catch { exception ->
                            _error.value = exception.localizedMessage
                            _isLoading.value = false
                            Timber.e(exception, "Error fetching nearby destinations")
                        }
                        .collect { result ->
                            _isLoading.value = false
                            result.fold(
                                onSuccess = { destinations ->
                                    // Sort destinations by distance before exposing to UI
                                    val sortedDestinations = sortDestinationsByDistance(destinations)
                                    _nearbyDestinations.value = sortedDestinations
                                },
                                onFailure = { exception ->
                                    _error.value = exception.localizedMessage
                                    Timber.e(exception, "Error processing nearby destinations")
                                }
                            )
                        }
                } ?: run {
                    // Fallback when useCase is null (temporary until dependency injection is fully implemented)
                    Timber.w("GetNearbyDestinationsUseCase not injected yet")
                }
            }
        }
    }

    // Try multiple times to get an accurate location
    private fun getLocationWithRetry() {
        if (!_locationPermissionGranted.value) {
            Timber.d("LocationViewModel: Cannot get location - permission not granted")
            return
        }
        
        isRequestingLocation = true
        Timber.d("LocationViewModel: Starting location updates with retry")
        
        // Start regular location updates
        startLocationUpdates()
        
        // Make additional attempts to get location if needed
        viewModelScope.launch {
            // First attempt immediately
            getLastKnownLocation()
            
            // Wait a bit for the first location update
            delay(1000)
            
            // If we still don't have a location after the delay, try again with high accuracy
            if (_currentLocation.value == null || isUsingDefaultLocation) {
                Timber.d("LocationViewModel: No real location received yet, requesting high accuracy updates")
                requestHighAccuracyLocationUpdates()
                
                // Wait a bit more and try last resort
                delay(2000)
                if (_currentLocation.value == null || isUsingDefaultLocation) {
                    Timber.d("LocationViewModel: Still no real location, trying one more time")
                    getLastKnownLocation()
                }
            }
        }
    }

    // Request location updates with high accuracy
    @SuppressLint("MissingPermission")
    private fun requestHighAccuracyLocationUpdates() {
        if (_locationPermissionGranted.value) {
            val highAccuracyRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(5000)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    highAccuracyRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                Timber.d("LocationViewModel: Requested high accuracy location updates")
            } catch (e: Exception) {
                Timber.e(e, "Error requesting high accuracy location updates")
            }
        }
    }

    // Get last known location
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        if (_locationPermissionGranted.value) {
            try {
                isRequestingLocation = true
                Timber.d("LocationViewModel: Getting last known location")
                
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            Timber.d("LocationViewModel: Successfully got last location: ${it.latitude}, ${it.longitude}")
                            updateLocationState(it)
                            isUsingDefaultLocation = false
                            isRequestingLocation = false
                            _isRealLocation.value = true
                            // Increment trigger to force UI update
                            _locationUpdateTrigger.value += 1
                        } ?: run {
                            // Only start location updates if we don't have a location yet
                            Timber.d("LocationViewModel: Last known location is null, starting location updates")
                            startLocationUpdates()
                            
                            // Only use default as a temporary fallback if we haven't received a location
                            // and we've already tried requesting one
                            if (_currentLocation.value == null) {
                                Timber.d("LocationViewModel: Temporarily using default location while waiting for real location")
                                _currentLocation.value = defaultLocation
                                _currentCityName.value = defaultCityName
                                isUsingDefaultLocation = true
                                _isRealLocation.value = false
                                // Increment trigger to force UI update
                                _locationUpdateTrigger.value += 1
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "LocationViewModel: Error getting last location")
                        // Only use default if we encounter an error getting location
                        if (_currentLocation.value == null) {
                            Timber.d("LocationViewModel: Error getting last known location, temporarily using default")
                            _currentLocation.value = defaultLocation
                            _currentCityName.value = defaultCityName
                            isUsingDefaultLocation = true
                            _isRealLocation.value = false
                            // Increment trigger to force UI update
                            _locationUpdateTrigger.value += 1
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "LocationViewModel: Exception getting last known location")
                // Only use default if we encounter an error getting location
                if (_currentLocation.value == null) {
                    Timber.d("LocationViewModel: Error getting last known location, temporarily using default")
                    _currentLocation.value = defaultLocation
                    _currentCityName.value = defaultCityName
                    isUsingDefaultLocation = true
                    _isRealLocation.value = false
                    // Increment trigger to force UI update
                    _locationUpdateTrigger.value += 1
                }
            }
        } else {
            // If permission not granted, we need to use default temporarily
            Timber.d("LocationViewModel: Location permission not granted, using default location")
            _currentLocation.value = defaultLocation
            _currentCityName.value = defaultCityName
            isUsingDefaultLocation = true
            _isRealLocation.value = false
            // Increment trigger to force UI update
            _locationUpdateTrigger.value += 1
        }
    }

    // Start continuous location updates
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (_locationPermissionGranted.value) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(true) // Wait for accurate location
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                Timber.d("LocationViewModel: Started location updates")
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                Timber.e(e, "LocationViewModel: Error starting location updates")
            }
        } else {
            Timber.d("LocationViewModel: Cannot start location updates - permission not granted")
        }
    }

    // Stop location updates to save battery
    fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Timber.d("LocationViewModel: Stopped location updates")
        } catch (e: Exception) {
            Timber.e(e, "LocationViewModel: Error stopping location updates")
        }
    }

    // Update location state and get city name through reverse geocoding
    private fun updateLocationState(location: Location) {
        viewModelScope.launch {
            val newLocation = LatLng(location.latitude, location.longitude)
            
            // Only update if this is a new location or we were using default before
            if (_currentLocation.value != newLocation || isUsingDefaultLocation) {
                _currentLocation.value = newLocation
                isUsingDefaultLocation = false
                _isRealLocation.value = true
                
                // Increment trigger to force UI update
                _locationUpdateTrigger.value += 1
                
                Timber.d("LocationViewModel: Location updated: ${location.latitude}, ${location.longitude}")

                // When location changes, update nearby destinations
                updateNearbyDestinations()

                // Update city name through reverse geocoding
                getCityNameFromLocation(location.latitude, location.longitude)
            }
        }
    }

    // Get city name from coordinates using reverse geocoding
    private fun getCityNameFromLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Use the new API for Android 13+
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        processCityFromAddresses(addresses)
                    }
                } else {
                    // Use the deprecated method for older Android versions
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    processCityFromAddresses(addresses)
                }
            } catch (e: Exception) {
                // If geocoding fails, keep current city name or use default
                if (_currentCityName.value.isEmpty()) {
                    _currentCityName.value = defaultCityName
                }
                Timber.e(e, "LocationViewModel: Error during reverse geocoding")
            }
        }
    }

    // Process address list to extract city name
    private fun processCityFromAddresses(addresses: List<Address>?) {
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            // Try to get locality (city) name, then admin area (state/province),
            // or use a default if both are null
            val cityName = when {
                !address.locality.isNullOrEmpty() -> address.locality
                !address.subAdminArea.isNullOrEmpty() -> address.subAdminArea
                !address.adminArea.isNullOrEmpty() -> address.adminArea
                else -> defaultCityName
            }
            _currentCityName.value = cityName
            Timber.d("LocationViewModel: City name updated: $cityName")
        } else {
            // If no addresses found, keep current city name or use default
            if (_currentCityName.value.isEmpty()) {
                _currentCityName.value = defaultCityName
                Timber.d("LocationViewModel: No addresses found, using default city name")
            }
        }
    }

    // Helper function to sort destinations by distance in ascending order
    private fun sortDestinationsByDistance(destinations: List<Destination>): List<Destination> {
        return if (_currentLocation.value != null) {
            destinations.sortedBy { destination ->
                // Extract numeric distance value from the distance string (e.g., "3.2 km" -> 3.2)
                val distanceStr = destination.distance
                try {
                    // Extract numeric part from strings like "3.2 km"
                    val numericPart = distanceStr.split(" ").firstOrNull()?.toDoubleOrNull() ?: Double.MAX_VALUE
                    numericPart
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing distance value: $distanceStr")
                    Double.MAX_VALUE // Put entries with unparseable distances at the end
                }
            }
        } else {
            destinations // If no current location, return original order
        }
    }

    // Clear error message
    fun clearError() {
        _error.value = null
    }

    // Clean up when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        Timber.d("LocationViewModel: ViewModel cleared, location updates stopped")
    }
}