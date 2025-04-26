package com.example.childsafe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childsafe.R
import com.example.childsafe.data.model.Destination
import com.example.childsafe.ui.components.BottomNavigationButtons
import com.example.childsafe.ui.components.GPSIndicator
import com.example.childsafe.ui.components.LocationSelectionPanel
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
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Screen for selecting a destination
 * Shows a map and destination selection options
 */
@Composable
fun DestinationSelectionScreen(
    selectedDestination: Destination? = null,
    onNavigateBack: () -> Unit,
    onDestinationSelected: (Destination) -> Unit,
    onSOSClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    locationViewModel: LocationViewModel = hiltViewModel()
) {
    // Coroutine scope for launching operations
    val coroutineScope = rememberCoroutineScope()
    
    // Get state from ViewModel
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val cityName by locationViewModel.currentCityName.collectAsState()
    val searchQuery by locationViewModel.searchQuery.collectAsState()
    val isRealLocation by locationViewModel.isRealLocation.collectAsState()
    val locationUpdateTrigger by locationViewModel.locationUpdateTrigger.collectAsState()
    
    // Local state variables
    var isWaitingForLocation by remember { mutableStateOf(currentLocation == null) }
    var hasCameraMoved by remember { mutableStateOf(false) }
    
    // State for destination marker
    var selectedDestinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedDestinationName by remember { mutableStateOf<String?>(null) }
    var selectedDestinationAddress by remember { mutableStateOf<String?>(null) }
    var tempDestination by remember { mutableStateOf<Destination?>(null) }
    var showBothLocations by remember { mutableStateOf(false) }
    var showConfirmButton by remember { mutableStateOf(false) }
    
    // State for collapsible panel
    var isPanelExpanded by remember { mutableStateOf(true) }
    
    val cameraPositionState = rememberCameraPositionState()

    // Force location update when screen is displayed
    LaunchedEffect(Unit) {
        Timber.d("DestinationSelectionScreen first display - requesting location")
        locationViewModel.getLastKnownLocation()
    }
    
    // Automatically collapse panel when destination is selected
    LaunchedEffect(tempDestination) {
        if (tempDestination != null) {
            isPanelExpanded = false
        }
    }
    
    // Continuously request location updates every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // 10 seconds
            Timber.d("Periodic location update request")
            locationViewModel.getLastKnownLocation()
        }
    }
    
    // Update from parent-provided selectedDestination if any
    LaunchedEffect(selectedDestination) {
        selectedDestination?.let { destination ->
            Timber.d("Received selectedDestination: ${destination.name}")
            
            // First try to get latLng directly
            val destinationLatLng = destination.latLng
            if (destinationLatLng != null) {
                Timber.d("Using latLng from destination: $destinationLatLng")
                selectedDestinationLatLng = destinationLatLng
                selectedDestinationName = destination.name
                selectedDestinationAddress = destination.address
                tempDestination = destination
                showBothLocations = currentLocation != null
                showConfirmButton = true
                isPanelExpanded = false
            } 
            // Otherwise try coordinates
            else if (destination.coordinates != null) {
                val latLng = destination.coordinates.toLatLng()
                Timber.d("Using coordinates from destination: $latLng")
                selectedDestinationLatLng = latLng
                selectedDestinationName = destination.name
                selectedDestinationAddress = destination.address
                tempDestination = destination
                showBothLocations = currentLocation != null
                showConfirmButton = true
                isPanelExpanded = false
            }
            
            // If we have both points, adjust camera
            if (showBothLocations && currentLocation != null && selectedDestinationLatLng != null) {
                try {
                    moveCameraToShowBothLocations(
                        cameraPositionState = cameraPositionState,
                        currentLocation = currentLocation!!,
                        destinationLocation = selectedDestinationLatLng!!
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error adjusting camera for both points")
                }
            }
        }
    }
    
    // Make sure we keep getting location updates
    DisposableEffect(Unit) {
        locationViewModel.startLocationUpdates()
        onDispose {
            locationViewModel.stopLocationUpdates()
        }
    }

    // React to location update triggers
    LaunchedEffect(locationUpdateTrigger) {
        Timber.d("Location update trigger fired: $locationUpdateTrigger")
        isWaitingForLocation = false
        
        currentLocation?.let { location ->
            Timber.d("Moving camera to location: $location, isReal=$isRealLocation, showBoth=$showBothLocations")
            
            if (showBothLocations && selectedDestinationLatLng != null) {
                // If we're showing both points, update the camera to include both
                try {
                    moveCameraToShowBothLocations(
                        cameraPositionState = cameraPositionState,
                        currentLocation = location,
                        destinationLocation = selectedDestinationLatLng!!
                    )
                    hasCameraMoved = true
                } catch (e: Exception) {
                    Timber.e(e, "Error updating camera for both locations")
                    // Fallback to current location only
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                }
            } else if (!hasCameraMoved || isRealLocation) {
                // Just focus on current location
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                hasCameraMoved = true
                Timber.d("Camera moved to current location: $location")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map background
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
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
            if (selectedDestinationLatLng != null) {
                Marker(
                    state = MarkerState(position = selectedDestinationLatLng!!),
                    title = selectedDestinationName ?: stringResource(R.string.destination),
                    snippet = selectedDestinationAddress,
                    // Use red color for destination
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
                Timber.d("Showing destination marker at $selectedDestinationLatLng")
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
                if (showBothLocations && selectedDestinationLatLng != null && currentLocation != null) {
                    // If showing both locations, adjust camera to show both points
                    try {
                        moveCameraToShowBothLocations(
                            cameraPositionState = cameraPositionState,
                            currentLocation = currentLocation!!,
                            destinationLocation = selectedDestinationLatLng!!
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

        // Confirm button - positioned at center-top when destination selected
        if (showConfirmButton && tempDestination != null) {
            Button(
                onClick = { 
                    Timber.d("Confirm destination button clicked")
                    onDestinationSelected(tempDestination!!) 
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp) // Positioned below city name
                    .padding(horizontal = AppDimensions.spacingMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
                Text("Confirm Destination")
            }
        }

        // Collapsible bottom panel for destination selection
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Selected destination mini-summary (always visible when destination selected)
            if (tempDestination != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Background,
                    shadowElevation = 4.dp,
                    shape = RoundedCornerShape(topStart = AppDimensions.panelCornerRadius, topEnd = AppDimensions.panelCornerRadius)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPanelExpanded = !isPanelExpanded }
                            .padding(AppDimensions.spacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected Destination:",
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimensions.textMedium
                            )
                            Text(
                                text = selectedDestinationName ?: "",
                                fontSize = AppDimensions.textMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = selectedDestinationAddress ?: "",
                                fontSize = AppDimensions.textSmall,
                                color = AppColors.GpsInactive
                            )
                        }
                        Icon(
                            imageVector = if (isPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isPanelExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Expandable panel content
            AnimatedVisibility(
                visible = isPanelExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Background,
                    shadowElevation = if (tempDestination == null) 4.dp else 0.dp,
                    shape = if (tempDestination == null) 
                        RoundedCornerShape(topStart = AppDimensions.panelCornerRadius, topEnd = AppDimensions.panelCornerRadius)
                    else
                        RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimensions.spacingMedium)
                    ) {
                        // Location panel with height constraint to ensure it doesn't take up too much space
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            LocationSelectionPanel(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { locationViewModel.updateSearchQuery(it) },
                                onDestinationSelected = { destination ->
                                    // Log the destination and extract location data
                                    Timber.d("Destination selected: ${destination.name}, latLng=${destination.latLng}, coordinates=${destination.coordinates}")
                                    
                                    // Handle the destination selection in a coroutine to avoid UI freezes
                                    coroutineScope.launch {
                                        try {
                                            // Extract LatLng from destination
                                            val destLatLng = when {
                                                destination.latLng != null -> {
                                                    Timber.d("Using latLng directly: ${destination.latLng}")
                                                    destination.latLng
                                                }
                                                destination.coordinates != null -> {
                                                    Timber.d("Converting coordinates: ${destination.coordinates}")
                                                    destination.coordinates.toLatLng()
                                                }
                                                else -> {
                                                    Timber.e("No location data in destination!")
                                                    null
                                                }
                                            }
                                            
                                            // Update marker state if we have location data
                                            if (destLatLng != null) {
                                                Timber.d("Setting destination marker at $destLatLng")
                                                selectedDestinationLatLng = destLatLng
                                                selectedDestinationName = destination.name
                                                selectedDestinationAddress = destination.address
                                                tempDestination = destination
                                                showBothLocations = currentLocation != null
                                                showConfirmButton = true
                                                isPanelExpanded = false
                                                
                                                // If we have both locations, adjust camera
                                                if (showBothLocations && currentLocation != null) {
                                                    Timber.d("Adjusting camera to show both locations")
                                                    moveCameraToShowBothLocations(
                                                        cameraPositionState = cameraPositionState,
                                                        currentLocation = currentLocation!!,
                                                        destinationLocation = destLatLng
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Timber.e(e, "Error handling destination selection")
                                        }
                                    }
                                },
                                currentLocation = currentLocation,
                                locationViewModel = locationViewModel
                            )
                        }

                        // Bottom navigation buttons always visible
                        BottomNavigationButtons(
                            modifier = Modifier,
                            onSOSClick = onSOSClick,
                            onNavigateClick = { /* Already in destination screen */ },
                            onProfileClick = onProfileClick
                        )
                    }
                }
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
        
        // Move camera to show the bounds with padding
        val padding = 300 // pixels of padding around the bounds
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        
        // Use position update for immediate effect
        cameraPositionState.move(cameraUpdate)
        
        Timber.d("Camera moved to show both current location and destination")
    } catch (e: Exception) {
        Timber.e(e, "Error moving camera to show both locations")
        
        // Fallback to a default zoom that might show both points
        val midLat = (currentLocation.latitude + destinationLocation.latitude) / 2
        val midLng = (currentLocation.longitude + destinationLocation.longitude) / 2
        val midPoint = LatLng(midLat, midLng)
        
        // Use a lower zoom level to increase the chance of showing both points
        cameraPositionState.position = CameraPosition.fromLatLngZoom(midPoint, 12f)
        Timber.d("Used fallback camera positioning to midpoint with zoom level 12")
    }
}