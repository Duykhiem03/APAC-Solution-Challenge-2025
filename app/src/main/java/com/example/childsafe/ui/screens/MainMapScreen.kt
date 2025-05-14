package com.example.childsafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childsafe.R
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Destination
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.domain.model.navigation.Route
import com.example.childsafe.ui.components.BottomNavigationButtons
import com.example.childsafe.ui.components.ChatListPanel
import com.example.childsafe.ui.components.GPSIndicator
import com.example.childsafe.ui.components.LocationSelectionPanel
import com.example.childsafe.ui.navigation.NavigationViewModel
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.AppDimensions
import com.example.childsafe.ui.viewmodel.ChatViewModel
import com.example.childsafe.ui.viewmodel.FriendsViewModel
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
 * Main map screen showing user's current location, destination, and safety information
 */
@Composable
fun MainMapScreen(
    onNavigateToDestination: () -> Unit = {},
    onSOSClick: () -> Unit = {},
    onWalkingTrackingClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onConversationSelected: (String) -> Unit = {},
    onUserSearchClick: () -> Unit = {},
    selectedDestination: Destination? = null,
    locationViewModel: LocationViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    navigationViewModel: NavigationViewModel = hiltViewModel(),
    friendsViewModel: FriendsViewModel = hiltViewModel()
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
    
    // Track if we're waiting for location by remember { mutableStateOf(currentLocation == null) }
    var hasCameraMoved by remember { mutableStateOf(false) }
    // Track if we're waiting for the location to be determined
    var isWaitingForLocation by remember { mutableStateOf(currentLocation == null) }    
    
    // Chat state
    var showChatPanel by remember { mutableStateOf(false) }
    
    // Location selection state - new for integrating LocationSelectionPanel
    var showLocationPanel by remember { mutableStateOf(false) }
    val searchQuery by locationViewModel.searchQuery.collectAsState()
    
    // Use a mutableState to track loading status separately
    var isManuallyLoadingChats by remember { mutableStateOf(false) }
    
    // Collect chat UI state
    val chatUiState by chatViewModel.uiState.collectAsState()
    
    // Track conversations as a derived value that we can debug
    val conversations = chatUiState.conversations.also {
        if (it.isNotEmpty()) {
            Timber.d("MainMapScreen: chatUiState has ${it.size} conversations")
        }
    }
    val userChats = chatUiState.userChats
    val isLoadingChats = chatUiState.isLoading || isManuallyLoadingChats
      // Force initialization of ChatViewModel on startup to ensure it loads data
    LaunchedEffect(Unit) {
        Timber.d("MainMapScreen: Initial load of conversations")
        isManuallyLoadingChats = true
        chatViewModel.loadConversations()
        delay(300) // Small delay
        chatViewModel.forceRefreshConversations() // Use our direct access method
        delay(300) // Small delay after force refresh
        chatViewModel.debugState() // Debug the state
        isManuallyLoadingChats = false
    }
    
    // Add debug logging to verify conversations
    LaunchedEffect(showChatPanel, conversations) {
        if (showChatPanel) {
            Timber.d("MainMapScreen: Chat panel is visible, conversation count: ${conversations.size}")
            conversations.forEachIndexed { index, conversation ->
                Timber.d("MainMapScreen: Conversation #${index+1}: id=${conversation.id}, lastMsg=${conversation.lastMessage?.text ?: "none"}")
            }
        }
    }
    
    // Add a specific effect to log when showChatPanel changes
    LaunchedEffect(showChatPanel) {
        Timber.d("MainMapScreen: showChatPanel changed to $showChatPanel")
    }
    
    // Add a specific effect to log when showLocationPanel changes
    LaunchedEffect(showLocationPanel) {
        Timber.d("MainMapScreen: showLocationPanel changed to $showLocationPanel")
        
        // Refresh search results when the panel is shown
        if (showLocationPanel) {
            locationViewModel.updateSearchQuery("")
        }
    }
    
      // Use mutableState to track selected destination so it can be updated when user makes a selection
    var activeDestination by remember(selectedDestination) { 
        mutableStateOf(selectedDestination)
    }
    
    // Extract destination location if available
    val destinationLatLng = remember(activeDestination) {
        activeDestination?.latLng ?: activeDestination?.coordinates?.toLatLng()
    }
    
    // Determine if we should show both current location and destination
    val showBothLocations = remember(currentLocation, destinationLatLng) {
        currentLocation != null && destinationLatLng != null
    }
    // Load chats when the chat panel is shown
    LaunchedEffect(showChatPanel) {
        Timber.d("MainMapScreen: showChatPanel changed to $showChatPanel")
        if (showChatPanel) {
            Timber.d("MainMapScreen: Loading conversations...")
            isManuallyLoadingChats = true
            chatViewModel.loadConversations()
            delay(500) // Give time for changes to propagate
            chatViewModel.debugState() // Call our new debug function
            isManuallyLoadingChats = false
        }
    }
    
    // Log state for debugging
    LaunchedEffect(selectedDestination) {
        Timber.d("MainMapScreen: Selected destination: ${selectedDestination?.name}, location: $destinationLatLng")
    }
    
    // Debug effect to log each state change in chatUiState
    LaunchedEffect(chatUiState) {
        Timber.d("MainMapScreen: chatUiState updated - isLoading: ${chatUiState.isLoading}, conversations: ${chatUiState.conversations.size}")
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
    LaunchedEffect(currentLocation, destinationLatLng, activeDestination) {
        if (currentLocation != null && destinationLatLng != null) {
            Timber.d("Requesting route from $currentLocation to $destinationLatLng for destination ${activeDestination?.name ?: "unknown"}")
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
                .padding(
                    vertical = AppDimensions.spacingXXLarge,
                    horizontal = AppDimensions.spacingMedium
                ),
            fontSize = AppDimensions.textTitle,
            fontWeight = FontWeight.Bold
        )

        // New top right buttons in column
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    vertical = AppDimensions.spacingXXLarge,
                    horizontal = AppDimensions.spacingMedium
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingSmall)
        ) {
            // First top-right button
            Box(
                modifier = Modifier
                    .size(AppDimensions.buttonSize)
                    .clip(CircleShape)
                    .background(AppColors.Background)
                    .border(1.dp, AppColors.OnSecondary, CircleShape)
                    .clickable {
                        // Add your action for the first button here
                        onProfileClick()
                    }
                    .padding(AppDimensions.spacingSmall),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonPin,
                    contentDescription = stringResource(R.string.profile),
                    tint = AppColors.OnSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.spacingSmall))
            // Second top-right button
            Box(
                modifier = Modifier
                    .size(AppDimensions.buttonSize)
                    .clip(CircleShape)
                    .background(Color(0xFF89CFF0))
                    .border(1.dp, AppColors.OnSecondary, CircleShape)
                    .clickable {
                        // Toggle the chat panel when profile button is clicked
                        val newShowChatPanel = !showChatPanel
                        showChatPanel = newShowChatPanel

                        // If showing chat panel, make sure location panel is hidden
                        if (newShowChatPanel) {
                            showLocationPanel = false
                            isManuallyLoadingChats = true
                            chatViewModel.forceRefreshConversations()

                            // The loading state will be reset by the LaunchedEffect(showChatPanel)
                        }
                    }
                    .padding(AppDimensions.spacingSmall),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SupervisorAccount,
                    contentDescription = stringResource(R.string.message),
                    tint = AppColors.OnSecondary
                )
            }
        }

        // GPS indicator moved to bottom right
        GPSIndicator(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = AppDimensions.spacingMedium), // Add padding from bottom to place above navigation buttons
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
        )        // Bottom navigation buttons
        BottomNavigationButtons(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppDimensions.spacingXLarge),
            onSOSClick = onSOSClick,
            onNavigateClick = {
                // Toggle location selection panel instead of navigating to a separate screen
                showLocationPanel = !showLocationPanel

                // Hide chat panel if showing location panel
                if (showLocationPanel) {
                    showChatPanel = false
                    locationViewModel.updateSearchQuery("")
                }
            },
            onWalkingTrackingClick = onWalkingTrackingClick
        )
          // Route information panel (shows when a route is available)
        navigationState.selectedRoute?.let { route ->
            // Only show when we have both current location and destination
            // Don't show when location panel is open to avoid UI conflicts
            if (showBothLocations && activeDestination != null && !showLocationPanel) {
                RouteInfoPanel(
                    route = route,
                    destinationName = activeDestination?.name,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp) // Add space above the navigation buttons
                        .padding(horizontal = 16.dp)
                )
            }
        }// Chat list panel (shows when user clicks on profile button)
        if (showChatPanel) {
            ChatListPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                conversations = conversations,
                userChats = userChats,
                onConversationSelected = { conversationId ->
                    onConversationSelected(conversationId)
                    showChatPanel = false // Hide panel after selection
                },
                onClose = { showChatPanel = false },
                isLoading = isLoadingChats,
                onCreateNewChat = {
                    onUserSearchClick()
                    showChatPanel = false // Hide panel after clicking create new chat
                },
                friendsViewModel = friendsViewModel,
                onStartChatWithFriend = { friendId ->
                    Timber.d("MainMapScreen: Starting chat with friend ID: $friendId")
                    friendsViewModel.startChatWithFriend(friendId) { conversationId ->
                        conversationId?.let {
                            onConversationSelected(it)
                            showChatPanel = false // Hide panel after selection
                        }
                    }
                }
            )
        }

        // Location Selection Panel (shows when user clicks on navigate button)
        if (showLocationPanel) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 500.dp)  // Maximum width for larger screens
                        .heightIn(min = 300.dp, max = 500.dp)  // Fixed height range
                ) {
                    LocationSelectionPanel(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { locationViewModel.updateSearchQuery(it) },
                        onDestinationSelected = { destination ->
                            // Update active destination
                            activeDestination = destination

                            // Extract LatLng from destination
                            val destLatLng = when {
                                destination.latLng != null -> destination.latLng
                                destination.coordinates != null -> destination.coordinates.toLatLng()
                                else -> null
                            }

                            if (destLatLng != null && currentLocation != null) {
                                // Request route calculation
                                navigationViewModel.getRoute(
                                    origin = currentLocation!!,
                                    destination = destLatLng,
                                    showAlternatives = false
                                )

                                // Move camera to show both locations
                                try {
                                    moveCameraToShowBothLocations(
                                        cameraPositionState = cameraPositionState,
                                        currentLocation = currentLocation!!,
                                        destinationLocation = destLatLng
                                    )
                                } catch (e: Exception) {
                                    Timber.e(e, "Error adjusting camera for both points")
                                }
                                // Set the selected destination and notify parent component
                                activeDestination = destination

                                // Call the navigation handler
                                onNavigateToDestination()

                                // Hide the panel after selection
                                showLocationPanel = false
                            }
                        },
                        currentLocation = currentLocation,
                        locationViewModel = locationViewModel,
                        onNavigateToDestination = {
                            showLocationPanel = false
                        }
                    )
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