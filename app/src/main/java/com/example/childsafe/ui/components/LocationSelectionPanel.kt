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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
    locationViewModel: LocationViewModel,
    onNavigateToDestination: (() -> Unit)? = null,
) {
    // Collect search results from the ViewModel
    val searchResults by locationViewModel.searchResults.collectAsState()
    val isLoading by locationViewModel.isLoading.collectAsState()
    val selectedPlaceType by locationViewModel.selectedPlaceType.collectAsState()
    
    // Add effect to log when search results change
    LaunchedEffect(searchResults) {
        Timber.d("Search results updated: ${searchResults.size} items found")
    }

    // Create a fixed-size panel with cream background as shown in the image
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFEF7EC), // Light cream/beige background from image
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        // Current location row with blue radio button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.RadioButton(
                selected = true,
                onClick = { /* Select current location */ },
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2196F3) // Blue color for radio button
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Vị trí hiện tại", // "Current location" in Vietnamese
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color.Black
            )
        }

        // Search field with red border and search icon
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            placeholder = { 
                Text(
                    text = "Bạn muốn đến đâu?", // "Where do you want to go?" in Vietnamese
                    color = Color.Gray
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Red
                )
            },
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Red.copy(alpha = 0.5f),
                cursorColor = Color.Red
            ),
            singleLine = true
        )
        
        // Add place type selector for filtering
        LocationPlaceTypeSelector(
            selectedType = selectedPlaceType,
            onTypeSelected = { locationViewModel.updateSelectedPlaceType(it) },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Divider
        Divider(
            color = Color.LightGray.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Location items - scrollable list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Show loading indicator when searching
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3),
                        modifier = Modifier.size(36.dp)
                    )
                }
            } 
            // Show search results if query is not empty
            else if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                // Display header for search results
                Text(
                    text = "Kết quả tìm kiếm", // "Search Results" in Vietnamese
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Display dynamic search results
                searchResults.forEach { destination ->
                    LocationItem(
                        destination = destination,
                        onDestinationSelected = onDestinationSelected
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
            // Show message if no results found
            else if (searchQuery.isNotEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy kết quả", // "No results found" in Vietnamese
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
            // Show nearby locations when not searching
            else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đang gần đây", // "Nearby" in Vietnamese
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = "5/6 km",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Default locations when not searching
                LocationItem(
                    destination = Destination(
                        id = 1,
                        name = "Bệnh viện Đa khoa Thành Phố Hồ Chí Minh",
                        address = "Quận 3, Thành phố Hồ Chí Minh • Cách 3,6 km",
                        distance = "3,6 km",
                        type = "hospital"
                    ),
                    onDestinationSelected = onDestinationSelected
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                
                LocationItem(
                    destination = Destination(
                        id = 2,
                        name = "Trường Đại học Sài Gòn - Cơ Sở 1",
                        address = "Quận 3, Thành phố Hồ Chí Minh • Cách 2,8 km",
                        distance = "2,8 km",
                        type = "university"
                    ),
                    onDestinationSelected = onDestinationSelected
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                
                LocationItem(
                    destination = Destination(
                        id = 3,
                        name = "Trường THPT Nguyễn Thị Minh Khai",
                        address = "Quận 3, Thành phố Hồ Chí Minh • Cách 2,4 km",
                        distance = "2,4 km",
                        type = "school"
                    ),
                    onDestinationSelected = onDestinationSelected
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                
                LocationItem(
                    destination = Destination(
                        id = 4,
                        name = "43 Ngô Thời Nhiệm",
                        address = "Quận 3, Thành phố Hồ Chí Minh • Cách 1,5 km",
                        distance = "1,5 km",
                        type = "address"
                    ),
                    onDestinationSelected = onDestinationSelected
                )
            }
        }
        
        // Add confirm location button if navigation callback is provided
        if (onNavigateToDestination != null) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = { onNavigateToDestination() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text(
                    text = "Xác nhận vị trí", // "Confirm location" in Vietnamese
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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

@Composable
fun LocationItem(
    destination: Destination,
    onDestinationSelected: (Destination) -> Unit
) {
    // Get the appropriate icon color based on location type
    val iconTint = when(destination.type) {
        "hospital" -> Color(0xFF4CAF50) // Green for hospital
        "school", "university" -> Color(0xFF2196F3) // Blue for educational institutions
        "address" -> Color(0xFFFF5722) // Orange for addresses
        else -> Color(0xFF2196F3) // Default blue
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDestinationSelected(destination) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Location icon with type-based color
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location",
            tint = iconTint,
            modifier = Modifier
                .padding(top = 2.dp, end = 12.dp)
                .size(22.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = destination.name,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = destination.address,
                fontSize = 14.sp,
                color = Color(0xFF757575),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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