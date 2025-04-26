package com.example.childsafe.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized color definitions for the ChildSafe app
 * This makes it easier to maintain a consistent color scheme
 */
object AppColors {
    // Primary colors
    val Primary = Color(0xFF3F51B5)
    val PrimaryVariant = Color(0xFF303F9F)
    val OnPrimary = Color(0xFFFFFF00)
    
    // Secondary colors
    val Secondary = Color(0xFFFF9800)
    val SecondaryVariant = Color(0xFFF57C00)
    val OnSecondary = Color.Black
    
    // Background colors
    val Background = Color.White
    val OnBackground = Color.Black
    
    // Surface colors
    val Surface = Color.White
    val OnSurface = Color.Black
    
    // Error colors
    val Error = Color(0xFFB00020)
    val OnError = Color.White
    
    // Additional colors
    val SosRed = Color(0xFFE53935)
    val GpsActive = Color(0xFF4CAF50)
    val GpsInactive = Color.Gray
    val ProfileYellow = Color(0xFFFFEB3B)
    val NavigateBlue = Color(0xFF2196F3)
    
    // Colors from Color.kt (migrated here)
    val CustomBlue = Color(0xFF1E88E5)
    val CustomGreen = Color(0xFF43A047)
    
    // Material defaults (kept for backward compatibility)
    val Purple80 = Color(0xFFD0BCFF)
    val PurpleGrey80 = Color(0xFFCCC2DC)
    val Pink80 = Color(0xFFEFB8C8)
    val Purple40 = Color(0xFF6650a4)
    val PurpleGrey40 = Color(0xFF625b71)
    val Pink40 = Color(0xFF7D5260)
}