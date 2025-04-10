package com.example.childsafe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.childsafe.R
import com.example.childsafe.data.repository.PlacesRepository.PlaceType
import com.example.childsafe.ui.theme.AppDimensions

/**
 * A component that displays selectable chips for different place types
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceTypeSelector(
    selectedType: PlaceType,
    onTypeSelected: (PlaceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.search_place_types),
            modifier = Modifier.padding(bottom = AppDimensions.spacingSmall)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingSmall)
        ) {
            // Create a chip for each place type
            PlaceTypeChip(
                placeType = PlaceType.ANY,
                selected = selectedType == PlaceType.ANY,
                onSelected = { onTypeSelected(PlaceType.ANY) }
            )
            
            PlaceTypeChip(
                placeType = PlaceType.SCHOOL,
                selected = selectedType == PlaceType.SCHOOL,
                onSelected = { onTypeSelected(PlaceType.SCHOOL) }
            )
            
            PlaceTypeChip(
                placeType = PlaceType.SHOPPING_MALL,
                selected = selectedType == PlaceType.SHOPPING_MALL,
                onSelected = { onTypeSelected(PlaceType.SHOPPING_MALL) }
            )
            
            PlaceTypeChip(
                placeType = PlaceType.RESTAURANT,
                selected = selectedType == PlaceType.RESTAURANT,
                onSelected = { onTypeSelected(PlaceType.RESTAURANT) }
            )
            
            PlaceTypeChip(
                placeType = PlaceType.HOSPITAL,
                selected = selectedType == PlaceType.HOSPITAL,
                onSelected = { onTypeSelected(PlaceType.HOSPITAL) }
            )
            
            PlaceTypeChip(
                placeType = PlaceType.PARK,
                selected = selectedType == PlaceType.PARK,
                onSelected = { onTypeSelected(PlaceType.PARK) }
            )
        }
    }
}

@Composable
private fun PlaceTypeChip(
    placeType: PlaceType,
    selected: Boolean,
    onSelected: () -> Unit
) {
    val label = when (placeType) {
        PlaceType.ANY -> stringResource(R.string.place_type_any)
        PlaceType.SCHOOL -> stringResource(R.string.place_type_school)
        PlaceType.SHOPPING_MALL -> stringResource(R.string.place_type_shopping_mall)
        PlaceType.RESTAURANT -> stringResource(R.string.place_type_restaurant)
        PlaceType.HOSPITAL -> stringResource(R.string.place_type_hospital)
        PlaceType.PARK -> stringResource(R.string.place_type_park)
    }
    
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = { Text(label) }
    )
}