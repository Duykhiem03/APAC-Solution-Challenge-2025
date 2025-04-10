package com.example.childsafe.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.childsafe.R
import com.example.childsafe.data.model.Destination
import com.example.childsafe.data.repository.PlacesRepository.PlaceType
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.AppDimensions
import com.example.childsafe.ui.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A panel that allows users to search for and select destinations
 * Users can drag the panel to resize it by dragging the handle at the top
 */
@Composable
fun LocationSelectionPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDestinationSelected: (Destination) -> Unit,
    currentLocation: LatLng?,
    locationViewModel: LocationViewModel
) {
    // Collect search results from the ViewModel - already sorted by distance
    val searchResults by locationViewModel.searchResults.collectAsState()
    val isLoading by locationViewModel.isLoading.collectAsState()
    val selectedPlaceType by locationViewModel.selectedPlaceType.collectAsState()
    
    // Get the screen height for maximum expansion
    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) { 
        configuration.screenHeightDp.dp 
    }
    
    // State for panel height control 
    val minHeight = 400.dp  // Smaller minimum height to make dragging more noticeable
    val maxHeight = screenHeight - 50.dp  // Maximum height that still shows a bit of the app bar
    
    // Remember the previous height between recompositions
    var currentHeight by remember { mutableStateOf(minHeight) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Use spring animation for more natural feel with faster response
    val animatedHeight by animateDpAsState(
        targetValue = currentHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "Height Animation"
    )
    
    // Density for converting pixels to dp
    val density = LocalDensity.current
    
    // Enhanced draggable state with more responsive updates
    val draggableState = rememberDraggableState { delta ->
        // Convert px delta to dp using saved density reference
        val dragDp = with(density) { delta.toDp() }
        // Negative delta means dragging up (increase height)
        val newHeight = (currentHeight - dragDp).coerceIn(minHeight, maxHeight)
        if (currentHeight != newHeight) {
            currentHeight = newHeight
            Timber.d("Panel resized: new height=$currentHeight")
        }
    }
    
    // Add a LaunchedEffect to reset the draggable state when recomposed
    LaunchedEffect(Unit) {
        // Start at minimum height when first launched
        currentHeight = minHeight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .background(
                color = AppColors.Background,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .shadow(
                elevation = if (isDragging) 8.dp else 4.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
    ) {
        // Resizer handle at the top of the panel - make it more prominent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp) // Increased touch target
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { isDragging = true },
                    onDragStopped = { isDragging = false }
                ),
            contentAlignment = Alignment.Center
        ) {
            // More visible draggable handle indicator
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 6.dp) // Wider and thicker
                    .clip(CircleShape)
                    .background(
                        if (isDragging) 
                            AppColors.NavigateBlue.copy(alpha = 0.9f)
                        else 
                            AppColors.GpsInactive.copy(alpha = 0.8f)
                    )
            )
        }
        
        // Rest of the panel content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp) // Add padding for better appearance
        ) {
            // Fixed top section with current location and search field
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background)
            ) {
                // Current location option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = true,
                        onClick = { /* Select current location */ }
                    )
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.current_location),
                        tint = if (currentLocation != null) AppColors.NavigateBlue else AppColors.GpsInactive
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
                    Text(text = stringResource(R.string.current_position))
                }

                // Destination search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimensions.spacingSmall),
                    placeholder = { Text(stringResource(R.string.destination_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    shape = RoundedCornerShape(AppDimensions.cardCornerRadius)
                )
            }
            
            // Light divider to separate fixed and scrollable sections
            Divider(color = AppColors.GpsInactive.copy(alpha = 0.2f))
            
            // Scrollable content area (search results or suggestions)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take remaining height
            ) {
                if (searchQuery.isNotBlank()) {
                    // Search mode - show place type selector and search results
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Place type selector
                        LocationPlaceTypeSelector(
                            selectedType = selectedPlaceType,
                            onTypeSelected = { placeType ->
                                locationViewModel.updateSelectedPlaceType(placeType)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppDimensions.spacingSmall)
                        )

                        // Show loading indicator if searching
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimensions.spacingMedium),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AppColors.NavigateBlue)
                            }
                        }

                        // Display search results if available - results are already sorted by the ViewModel
                        if (searchResults.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.search_results),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    top = AppDimensions.spacingMedium,
                                    bottom = AppDimensions.spacingSmall
                                )
                            )
                            
                            // Display search results in a scrollable list
                            Column(modifier = Modifier.fillMaxWidth()) {
                                searchResults.forEach { destination ->
                                    DestinationItem(
                                        destination = destination,
                                        onDestinationSelected = onDestinationSelected
                                    )
                                    Spacer(modifier = Modifier.height(AppDimensions.spacingSmall))
                                }
                            }
                        } else if (!isLoading) {
                            // No results found
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimensions.spacingMedium),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_results_found),
                                    color = AppColors.GpsInactive
                                )
                            }
                        }
                    }
                } else {
                    // Not searching - show suggested locations
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Toggle options when not searching
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppDimensions.spacingSmall),
                        ) {
                            Text(
                                text = stringResource(R.string.worth_visiting),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(AppDimensions.cardCornerRadius))
                                    .clickable { /* Toggle */ }
                                    .padding(AppDimensions.spacingSmall),
                                color = AppColors.NavigateBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimensions.textMedium
                            )
                            Text(
                                text = stringResource(R.string.saved_locations),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(AppDimensions.cardCornerRadius))
                                    .clickable { /* Toggle */ }
                                    .padding(AppDimensions.spacingSmall),
                                fontSize = AppDimensions.textMedium
                            )
                        }

                        // Suggested locations
                        SuggestedLocations(
                            onDestinationSelected = onDestinationSelected,
                            currentLocation = currentLocation
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable that displays selectable place type options for the location panel
 */
@Composable
fun LocationPlaceTypeSelector(
    selectedType: PlaceType,
    onTypeSelected: (PlaceType) -> Unit,
    modifier: Modifier = Modifier
) {
    val placeTypes = listOf(
        PlaceType.ANY to stringResource(R.string.place_type_any),
        PlaceType.HOSPITAL to stringResource(R.string.place_type_hospital),
        PlaceType.RESTAURANT to stringResource(R.string.place_type_restaurant),
        PlaceType.PARK to stringResource(R.string.place_type_park),
        PlaceType.SCHOOL to stringResource(R.string.place_type_school),
        PlaceType.SHOPPING_MALL to stringResource(R.string.place_type_shopping_mall),
    )
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppDimensions.spacingSmall)
    ) {
        placeTypes.forEach { pair ->
            val type = pair.first
            val label = pair.second
            val isSelected = type == selectedType
            Box(
                modifier = Modifier
                    .padding(end = AppDimensions.spacingSmall)
                    .clip(RoundedCornerShape(AppDimensions.cardCornerRadius))
                    .background(
                        if (isSelected) AppColors.NavigateBlue.copy(alpha = 0.1f)
                        else AppColors.GpsInactive.copy(alpha = 0.05f)
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(
                        horizontal = AppDimensions.spacingMedium,
                        vertical = AppDimensions.spacingSmall
                    )
            ) {
                Text(
                    text = label,
                    color = if (isSelected) AppColors.NavigateBlue else AppColors.Primary,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun SuggestedLocations(
    onDestinationSelected: (Destination) -> Unit,
    currentLocation: LatLng?
) {
    // Some predefined suggestions
    val destinations = listOf(
        Destination(
            id = 1,
            name = "Bệnh viện Đa Lâm Thành Phố Hồ Chí Minh",
            address = "201 Nguyễn Chí Thanh, P. 12, Quận 5, TP HCM, Việt Nam",
            distance = if (currentLocation != null) "3.2 km" else "Unknown"
        ),
        Destination(
            id = 2,
            name = "Trường Đại học Sài Gòn - Cơ Sở 1",
            address = "105 Bà Huyện Thanh Quan, Phường 7, Quận 3, Việt Nam",
            distance = if (currentLocation != null) "1.5 km" else "Unknown"
        ),
        Destination(
            id = 3,
            name = "Trường THPT Nguyễn Thị Minh Khai",
            address = "275 Điện Biên Phủ, Phường 7, Quận 3, TP HCM, Việt Nam",
            distance = if (currentLocation != null) "2.4 km" else "Unknown"
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        destinations.forEach { destination ->
            DestinationItem(
                destination = destination,
                onDestinationSelected = onDestinationSelected
            )
            Spacer(modifier = Modifier.height(AppDimensions.spacingSmall))
        }
    }
}

@Composable
fun DestinationItem(
    destination: Destination,
    onDestinationSelected: (Destination) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDestinationSelected(destination) }
            .padding(vertical = AppDimensions.spacingXSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = AppColors.NavigateBlue
            )
            Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = destination.address,
                    fontSize = AppDimensions.textSmall,
                    color = AppColors.GpsInactive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getCurrentTime(),
                    fontSize = AppDimensions.textSmall,
                    color = AppColors.GpsInactive
                )
            }
            Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
            Text(
                text = destination.distance,
                fontSize = AppDimensions.textSmall,
                color = AppColors.GpsInactive
            )
        }
    }
}

private fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date())
}

// Extension function to convert dp to pixels
@Composable
private fun Float.toDp() = with(LocalDensity.current) { this@toDp.toDp() }