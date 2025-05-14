package com.example.childsafe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.childsafe.R
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.AppDimensions

/**
 * A GPS indicator component that shows the status of GPS/location services
 * 
 * @param modifier The modifier to be applied to the component
 * @param isActive Whether the GPS is currently active
 * @param onClick Callback for when the indicator is clicked
 */
@Composable
fun GPSIndicator(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(AppDimensions.buttonSize)
            .clip(CircleShape)
            .background(AppColors.Background)
            .clickable(onClick = onClick)
            .padding(AppDimensions.spacingSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.gps),
            color = if (isActive) AppColors.GpsActive else AppColors.GpsInactive,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Bottom navigation buttons for the main screen
 * 
 * @param modifier The modifier to be applied to the component
 * @param onSOSClick Callback for when the SOS button is clicked
 * @param onNavigateClick Callback for when the Navigate button is clicked
 * @param onWalkingTrackingClick Callback for when the Walking Tracking button is clicked
 */
@Composable
fun BottomNavigationButtons(
    modifier: Modifier = Modifier,
    onSOSClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onWalkingTrackingClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimensions.spacingMedium),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Profile button
        Box(
            modifier = Modifier
                .size(AppDimensions.navigationButtonSize)
                .clip(CircleShape)
                .background(AppColors.ProfileYellow)
                .border(1.dp, AppColors.OnSecondary, CircleShape)
                .clickable { onWalkingTrackingClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.footstep),
                contentDescription = stringResource(R.string.profile),
                modifier = Modifier.size(24.dp),
            )
        }

        // SOS button
        Box(
            modifier = Modifier
                .size(AppDimensions.navigationButtonSize)
                .clip(CircleShape)
                .background(AppColors.SosRed)
                .border(1.dp, AppColors.OnSecondary, CircleShape)
                .clickable { onSOSClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.sos),
                color = AppColors.OnError,
                fontWeight = FontWeight.Bold
            )
        }

        // Navigate button
        Box(
            modifier = Modifier
                .size(AppDimensions.navigationButtonSize)
                .clip(CircleShape)
                .background(AppColors.Background)
                .border(1.dp, AppColors.OnSecondary, CircleShape)
                .clickable { onNavigateClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Navigation,
                contentDescription = stringResource(R.string.navigate),
                tint = AppColors.NavigateBlue
            )
        }
    }
}