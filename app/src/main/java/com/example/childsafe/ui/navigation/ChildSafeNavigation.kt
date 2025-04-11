package com.example.childsafe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.childsafe.OnboardingScreen
import com.example.childsafe.ProfileSettingScreen
import com.example.childsafe.data.model.Destination
import com.example.childsafe.ui.components.LocationPermissionHandler
import com.example.childsafe.ui.screens.DestinationSelectionScreen
import com.example.childsafe.ui.screens.MainMapScreen
import com.example.childsafe.ui.viewmodel.LocationViewModel
import timber.log.Timber


/**
 * Primary app navigation routes
 */
object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val PROFILE_SETTINGS = "profile_settings"
    const val MAIN_MAP = "main_map"
    const val DESTINATION_SELECTION = "destination_selection"
    const val SOS_SCREEN = "sos"
}

/**
 * Main navigation component for the app
 * Sets up navigation routes and handles navigation between screens
 */
@Composable
fun ChildSafeNavigation() {
    val navController = rememberNavController()
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    
    // Initialize location view model using Hilt
    val locationViewModel: LocationViewModel = hiltViewModel()
    val locationPermissionGranted by locationViewModel.locationPermissionGranted.collectAsState()

    // Handle location permissions - make sure to update the view model directly
    LocationPermissionHandler { isGranted -> 
        Timber.d("ChildSafeNavigation: Permission result from handler: granted=$isGranted")
        // Always update the ViewModel with the latest permission state
        locationViewModel.updateLocationPermissionStatus(isGranted)
    }

    NavHost(navController = navController, startDestination = NavRoutes.ONBOARDING) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(onGetStarted = { navController.navigate(NavRoutes.PROFILE_SETTINGS) })
        }
        
        composable(NavRoutes.PROFILE_SETTINGS) {
            ProfileSettingScreen(onComplete = { navController.navigate(NavRoutes.MAIN_MAP) })
        }
        
        composable(NavRoutes.MAIN_MAP) {
            // Pass the selected destination to the main map screen when returning from destination selection
            MainMapScreen(
                selectedDestination = selectedDestination,
                onNavigateToDestination = {
                    navController.navigate(NavRoutes.DESTINATION_SELECTION)
                },
                onSOSClick = {
                    navController.navigate(NavRoutes.SOS_SCREEN)
                },
                onProfileClick = {
                    navController.navigate(NavRoutes.PROFILE_SETTINGS)
                },
                // Pass locationViewModel explicitly to ensure consistency
                locationViewModel = locationViewModel
            )
        }
        
        composable(NavRoutes.DESTINATION_SELECTION) {
            DestinationSelectionScreen(
                selectedDestination = selectedDestination,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDestinationSelected = { destination -> 
                    Timber.d("Destination selected: ${destination.name}, latLng=${destination.latLng}, coordinates=${destination.coordinates}")
                    selectedDestination = destination
                    navController.popBackStack()
                },
                onSOSClick = {
                    navController.navigate(NavRoutes.SOS_SCREEN)
                },
                onProfileClick = {
                    navController.navigate(NavRoutes.PROFILE_SETTINGS)
                },
                // Pass locationViewModel explicitly to ensure consistency
                locationViewModel = locationViewModel
            )
        }
        
        composable(NavRoutes.SOS_SCREEN) {
            // SOSScreen will be implemented separately
            // SOSScreen(onBack = { navController.popBackStack() })
        }
    }
}