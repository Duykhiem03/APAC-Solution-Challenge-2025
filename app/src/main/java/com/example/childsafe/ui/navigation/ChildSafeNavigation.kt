package com.example.childsafe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.childsafe.OnboardingScreen
import com.example.childsafe.ProfileSettingScreen
import com.example.childsafe.auth.PhoneAuthScreen
import com.example.childsafe.auth.PhoneAuthViewModel
import com.example.childsafe.data.model.Destination
import com.example.childsafe.ui.components.LocationPermissionHandler
import com.example.childsafe.ui.screens.ChatScreen
import com.example.childsafe.ui.screens.MainMapScreen
import com.example.childsafe.ui.screens.WalkingTrackingScreen
import com.example.childsafe.ui.viewmodel.ChatViewModel
import com.example.childsafe.ui.viewmodel.LocationViewModel
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber


/**
 * Primary app navigation routes
 */
object NavRoutes {
    const val PHONE_AUTH = "phone_auth"
    const val ONBOARDING = "onboarding"
    const val PROFILE_SETTINGS = "profile_settings"
    const val MAIN_MAP = "main_map"
    const val SOS_SCREEN = "sos"
    const val CHAT_SCREEN = "chat/{conversationId}"
    const val WALKING_TRACKING = "walking_tracking"

    // Helper function to create the chat route with a specific conversation ID
    fun chatRoute(conversationId: String) = "chat/$conversationId"
}

/**
 * Main navigation component for the app
 * Sets up navigation routes and handles navigation between screens
 */
@Composable
fun ChildSafeNavigation() {
    val navController = rememberNavController()
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    
    // Initialize view models using Hilt
    val locationViewModel: LocationViewModel = hiltViewModel()
    val phoneAuthViewModel: PhoneAuthViewModel = hiltViewModel()
    val locationPermissionGranted by locationViewModel.locationPermissionGranted.collectAsState()
    val authState by phoneAuthViewModel.authState.collectAsState()
    
    // Check if user is already authenticated and track it with state
    val isUserAuthenticated = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
    
    // Track authentication changes from Firebase
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            isUserAuthenticated.value = auth.currentUser != null
            Timber.d("Auth state changed: user authenticated = ${isUserAuthenticated.value}")
        }
    }
    
    // Handle location permissions - make sure to update the view model directly
    LocationPermissionHandler { isGranted -> 
        Timber.d("ChildSafeNavigation: Permission result from handler: granted=$isGranted")
        // Always update the ViewModel with the latest permission state
        locationViewModel.updateLocationPermissionStatus(isGranted)
    }
    
    // Determine the start destination based on authentication state
    val startDestination = if (isUserAuthenticated.value) {
        NavRoutes.ONBOARDING
    } else {
        NavRoutes.PHONE_AUTH
    }
    
    // Check and redirect to authentication if needed
    fun NavController.navigateToProtectedRoute(route: String) {
        if (isUserAuthenticated.value) {
            this.navigate(route)
        } else {
            Timber.d("User not authenticated, redirecting to auth screen")
            this.navigate(NavRoutes.PHONE_AUTH) {
                this.popUpTo(this@navigateToProtectedRoute.currentDestination?.route ?: "") {
                    inclusive = true
                }
            }
        }
    }
    
    // Effect to watch auth state changes and navigate accordingly
    LaunchedEffect(isUserAuthenticated.value) {
        // If user logs out while in a protected route, redirect to auth
        if (!isUserAuthenticated.value && navController.currentDestination?.route != NavRoutes.PHONE_AUTH) {
            Timber.d("User logged out, redirecting to auth screen")
            navController.navigate(NavRoutes.PHONE_AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Phone authentication screen
        composable(NavRoutes.PHONE_AUTH) {
            PhoneAuthScreen(
                onAuthSuccess = {
                    isUserAuthenticated.value = true
                    navController.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.PHONE_AUTH) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(onGetStarted = { 
                if (isUserAuthenticated.value) {
                    navController.navigate(NavRoutes.PROFILE_SETTINGS)
                } else {
                    navController.navigate(NavRoutes.PHONE_AUTH)
                }
            })
        }
        
        composable(NavRoutes.PROFILE_SETTINGS) {
            // Check authentication before showing profile settings
            LaunchedEffect(Unit) {
                if (!isUserAuthenticated.value) {
                    navController.navigate(NavRoutes.PHONE_AUTH) {
                        popUpTo(NavRoutes.PROFILE_SETTINGS) { inclusive = true }
                    }
                }
            }
            
            ProfileSettingScreen(
                // Use Firebase user info instead of mock data
                onComplete = { navController.navigate(NavRoutes.MAIN_MAP) },
                onSignOut = {
                    // Handle sign out navigation - will be handled by the LaunchedEffect that 
                    // watches isUserAuthenticated.value in the NavHost
                    Timber.d("User signed out from ProfileSettingScreen")
                    // No need to navigate here as the auth state listener will handle it
                }
            )
        }
        
        composable(NavRoutes.MAIN_MAP) {
            // Check authentication before showing main map
            LaunchedEffect(Unit) {
                if (!isUserAuthenticated.value) {
                    navController.navigate(NavRoutes.PHONE_AUTH) {
                        popUpTo(NavRoutes.MAIN_MAP) { inclusive = true }
                    }
                }
            }
            
            // Main map screen with integrated destination selection
            MainMapScreen(
                selectedDestination = selectedDestination,
                onNavigateToDestination = {
                    // No longer navigating to a separate screen for destination selection
                    // This function is now used for confirming the selected destination
                    Timber.d("Destination confirmed: ${selectedDestination?.name ?: "None"}")
                },
                onSOSClick = {
                    if (isUserAuthenticated.value) {
                        navController.navigate(NavRoutes.SOS_SCREEN)
                    } else {
                        navController.navigate(NavRoutes.PHONE_AUTH)
                    }
                },
                onWalkingTrackingClick = {
                    if (isUserAuthenticated.value) {
                        navController.navigate(NavRoutes.WALKING_TRACKING)
                    } else {
                        navController.navigate(NavRoutes.PHONE_AUTH)
                    }
                },
                onProfileClick = {
                    if (isUserAuthenticated.value) {
                        navController.navigate(NavRoutes.PROFILE_SETTINGS)
                    } else {
                        navController.navigate(NavRoutes.PHONE_AUTH)
                    }
                },
                // Handle navigation to chat when a conversation is selected
                onConversationSelected = { conversationId ->
                    navController.navigate(NavRoutes.chatRoute(conversationId))
                },
                // Open the chat panel with friends tab when creating a new chat
                onUserSearchClick = {
                    // The friends functionality is now integrated into ChatListPanel
                    // No navigation needed, handled internally in MainMapScreen
                },
                // Pass locationViewModel explicitly to ensure consistency
                locationViewModel = locationViewModel
            )
        }

        
        // Chat screen with conversationId parameter
        composable(
            route = NavRoutes.CHAT_SCREEN,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Check authentication before showing chat
            LaunchedEffect(Unit) {
                if (!isUserAuthenticated.value) {
                    navController.navigate(NavRoutes.PHONE_AUTH) {
                        popUpTo(NavRoutes.CHAT_SCREEN) { inclusive = true }
                    }
                }
            }
            
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            
            ChatScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // SOS screen with authentication check
        composable(NavRoutes.SOS_SCREEN) {
            // Check authentication before showing SOS screen
            LaunchedEffect(Unit) {
                if (!isUserAuthenticated.value) {
                    navController.navigate(NavRoutes.PHONE_AUTH) {
                        popUpTo(NavRoutes.SOS_SCREEN) { inclusive = true }
                    }
                }
            }

        }
        
        // Note: User search and friend requests functionality has been integrated
        // directly into the ChatListPanel component in tabs
        
        // Walking Tracking screen with authentication check
        composable(NavRoutes.WALKING_TRACKING) {
            // Check authentication before showing walking tracking screen
            LaunchedEffect(Unit) {
                if (!isUserAuthenticated.value) {
                    navController.navigate(NavRoutes.PHONE_AUTH) {
                        popUpTo(NavRoutes.WALKING_TRACKING) { inclusive = true }
                    }
                }
            }
            
            WalkingTrackingScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}