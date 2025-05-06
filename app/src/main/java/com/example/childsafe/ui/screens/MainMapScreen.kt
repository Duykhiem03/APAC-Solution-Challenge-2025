package com.example.childsafe.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childsafe.R
import com.example.childsafe.data.model.Destination
import com.example.childsafe.domain.model.navigation.Route
import com.example.childsafe.ui.components.BottomNavigationButtons
import com.example.childsafe.ui.components.GPSIndicator
import com.example.childsafe.ui.navigation.NavigationViewModel
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.AppDimensions
import com.example.childsafe.ui.viewmodel.LocationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Main map screen showing user's current location and selected destination
 *
 * @param selectedDestination The destination that was selected by the user, if any
 * @param onNavigateToDestination Callback when user wants to navigate to a destination
 * @param onSOSClick Callback for when the SOS button is clicked
 * @param onProfileClick Callback for when the profile button is clicked
 * @param locationViewModel ViewModel for location data
 * @param navigationViewModel ViewModel for handling route calculations
 */
@Composable
fun MainMapScreen(
    selectedDestination: Destination? = null,
    onNavigateToDestination: (Destination) -> Unit,
    onSOSClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    locationViewModel: LocationViewModel = hiltViewModel(),
    navigationViewModel: NavigationViewModel = hiltViewModel()
) {
    // Get current location and city name from ViewModel
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val cityName by locationViewModel.currentCityName.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    val isRealLocation by locationViewModel.isRealLocation.collectAsState()
    // This will trigger UI updates when location changes
    val locationUpdateTrigger by locationViewModel.locationUpdateTrigger.collectAsState()
    
    // Navigation state
    val navigationState by navigationViewModel.uiState.collectAsState()
    // Track if we're showing route details
    var showRouteDetails by remember { mutableStateOf(false) }
    
    // Track if we're waiting for location
    var isWaitingForLocation by remember { mutableStateOf(currentLocation == null) }
    // Track if camera has been initially positioned
    var hasCameraMoved by remember { mutableStateOf(false) }
    
    // Extract destination location if available
    val destinationLatLng = remember(selectedDestination) {
        selectedDestination?.latLng ?: selectedDestination?.coordinates?.toLatLng()
    }
    
    // Determine if we should show both current location and destination
    val showBothLocations = remember(currentLocation, destinationLatLng) {
        currentLocation != null && destinationLatLng != null
    }
    
    // Log state for debugging
    LaunchedEffect(selectedDestination) {
        Timber.d("MainMapScreen: Selected destination: ${selectedDestination?.name}, location: $destinationLatLng")
    }

    // Force location update when screen is displayed
    LaunchedEffect(Unit) {
        Timber.d("MainMapScreen first display - requesting location")
        locationViewModel.getLastKnownLocation()
    }
    
    // Continuously request location updates every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // 10 seconds
            Timber.d("Periodic location update request")
            locationViewModel.getLastKnownLocation()
        }
    }
    
    // Make sure we keep getting location updates
    DisposableEffect(Unit) {
        locationViewModel.startLocationUpdates()
        onDispose {
            locationViewModel.stopLocationUpdates()
        }
    }
    
    // Calculate route when both current location and destination are available
    LaunchedEffect(currentLocation, destinationLatLng) {
        if (currentLocation != null && destinationLatLng != null) {
            Timber.d("Requesting route from $currentLocation to $destinationLatLng")
            navigationViewModel.getRoute(
                origin = currentLocation!!,
                destination = destinationLatLng,
                showAlternatives = false
            )
        }
    }

    // React to location update triggers
    LaunchedEffect(locationUpdateTrigger) {
        Timber.d("Location update trigger fired: $locationUpdateTrigger")
        isWaitingForLocation = false
        
        currentLocation?.let { location ->
            Timber.d("Moving camera to location: $location, isReal=$isRealLocation, showBoth=$showBothLocations")
            
            if (showBothLocations && destinationLatLng != null) {
                // If we have both current location and destination, show both
                try {
                    moveCameraToShowBothLocations(
                        cameraPositionState = cameraPositionState,
                        currentLocation = location,
                        destinationLocation = destinationLatLng
                    )
                    hasCameraMoved = true
                    Timber.d("Camera moved to show both current location and destination")
                } catch (e: Exception) {
                    Timber.e(e, "Error adjusting camera to show both locations")
                    // Fallback to showing just current location
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                }
            } else if (!hasCameraMoved || isRealLocation) {
                // Just show current location
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                hasCameraMoved = true
                Timber.d("Camera moved to current location: $location")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // GoogleMap showing user location and destination if available
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // Add UI settings to enable zoom controls and gestures
            uiSettings = androidx.compose.runtime.remember {
                com.google.maps.android.compose.MapUiSettings(
                    zoomControlsEnabled = true,     // Show zoom +/- buttons
                    zoomGesturesEnabled = true,     // Allow pinch to zoom
                    scrollGesturesEnabled = true,   // Allow panning with finger
                    rotationGesturesEnabled = true, // Allow two-finger rotation
                    tiltGesturesEnabled = true      // Allow tilt for 3D view
                )
            },
            // Set appropriate zoom limits for better user experience
            properties = com.google.maps.android.compose.MapProperties(
                minZoomPreference = 3f,  // Prevent zooming out too far
                maxZoomPreference = 21f, // Allow very detailed zoom
                isMyLocationEnabled = false // We handle our own location marker
            )
        ) {
            // Show current location marker if available
            currentLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = stringResource(R.string.current_location),
                    snippet = stringResource(R.string.you_are_here),
                    // Use blue color for current location
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
                Timber.d("Showing current location marker at $location")
            }
            
            // Show destination marker if available
            if (destinationLatLng != null) {
                Marker(
                    state = MarkerState(position = destinationLatLng),
                    title = selectedDestination?.name ?: stringResource(R.string.destination),
                    snippet = selectedDestination?.address,
                    // Use red color for destination
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
                Timber.d("Showing destination marker at $destinationLatLng")
            }
            
            // Show route polyline if available
            navigationState.selectedRoute?.let { route ->
                Polyline(
                    points = route.decodedPolyline(),
                    color = Color.Blue,
                    width = 8f
                )
                Timber.d("Showing route polyline with ${route.decodedPolyline().size} points")
            }
        }

        // Show loading indicator if we're waiting for first location
        if (isWaitingForLocation) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AppColors.Primary
            )
        }

        // City name overlay
        Text(
            text = cityName,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(vertical = AppDimensions.spacingXXLarge, horizontal = AppDimensions.spacingMedium),
            fontSize = AppDimensions.textTitle,
            fontWeight = FontWeight.Bold
        )

        // GPS indicator
        GPSIndicator(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppDimensions.spacingMedium),
            isActive = isRealLocation,
            onClick = {
                if (showBothLocations && destinationLatLng != null && currentLocation != null) {
                    // If showing both locations, adjust camera to show both points
                    try {
                        moveCameraToShowBothLocations(
                            cameraPositionState = cameraPositionState,
                            currentLocation = currentLocation!!,
                            destinationLocation = destinationLatLng
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error adjusting camera from GPS indicator")
                    }
                } else {
                    // Otherwise just center on current location
                    currentLocation?.let {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
                    } ?: run {
                        isWaitingForLocation = true
                        locationViewModel.getLastKnownLocation()
                    }
                }
            }
        )

        // Bottom navigation buttons
        BottomNavigationButtons(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppDimensions.spacingXLarge),
            onSOSClick = onSOSClick,
            onNavigateClick = { onNavigateToDestination(Destination()) },
            onProfileClick = onProfileClick
        )
        
        // Route information panel (shows when a route is available)
        navigationState.selectedRoute?.let { route ->
            // Only show when we have both current location and destination
            if (showBothLocations && selectedDestination != null) {
                RouteInfoPanel(
                    route = route,
                    destinationName = selectedDestination.name,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp) // Add space above the navigation buttons
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Moves the camera to show both the current location and destination location
 * with appropriate padding
 */
private fun moveCameraToShowBothLocations(
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    currentLocation: LatLng,
    destinationLocation: LatLng
) {
    try {
        // Create bounds that include both points
        val bounds = LatLngBounds.Builder()
            .include(currentLocation)
            .include(destinationLocation)
            .build()
        
        // Calculate a better padding based on screen size
        val displayMetrics = android.content.res.Resources.getSystem().displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val smallestDimension = kotlin.math.min(width, height)
        val padding = (smallestDimension * 0.15).toInt() // 15% of smallest dimension
        
        // Move camera to show the bounds with padding
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        
        // Use move() instead of animate() since move() is not a suspend function
        cameraPositionState.move(cameraUpdate)
        
        Timber.d("Camera moved to show both current location and destination with adaptive padding")
    } catch (e: Exception) {
        Timber.e(e, "Error moving camera to show both locations")
        
        // Improved fallback: calculate distance to determine appropriate zoom level
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            destinationLocation.latitude, destinationLocation.longitude,
            results
        )
        val distance = results[0]
        
        // Choose zoom level based on distance
        val zoomLevel = when {
            distance < 500 -> 16f    // Less than 500m
            distance < 1000 -> 15f   // Less than 1km
            distance < 5000 -> 13f   // Less than 5km
            distance < 10000 -> 12f  // Less than 10km
            distance < 50000 -> 10f  // Less than 50km
            else -> 8f               // Long distance
        }
        
        // Find midpoint between the two locations
        val midLat = (currentLocation.latitude + destinationLocation.latitude) / 2
        val midLng = (currentLocation.longitude + destinationLocation.longitude) / 2
        val midPoint = LatLng(midLat, midLng)
        
        // Use position assignment instead of animate()
        cameraPositionState.position = CameraPosition.Builder()
            .target(midPoint)
            .zoom(zoomLevel)
            .build()
            
        Timber.d("Used fallback camera positioning to midpoint with adaptive zoom level $zoomLevel")
    }
}

/**
 * Displays route information including distance and duration
 */
@Composable
private fun RouteInfoPanel(
    route: Route,
    destinationName: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Route to ${destinationName ?: "Destination"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Distance: ${route.distance.text}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = "Travel time: ${route.duration.text}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}