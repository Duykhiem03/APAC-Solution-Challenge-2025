package com.example.childsafe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme color scheme
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    primaryContainer = AppColors.PrimaryVariant,
    onPrimary = AppColors.OnPrimary,
    secondary = AppColors.Secondary,
    secondaryContainer = AppColors.SecondaryVariant,
    onSecondary = AppColors.OnSecondary,
    background = AppColors.Background,
    onBackground = AppColors.OnBackground,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    error = AppColors.Error,
    onError = AppColors.OnError
)

// Dark theme color scheme
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    primaryContainer = AppColors.PrimaryVariant,
    onPrimary = AppColors.OnPrimary,
    secondary = AppColors.Secondary,
    secondaryContainer = AppColors.SecondaryVariant,
    onSecondary = AppColors.OnSecondary,
    background = AppColors.Background,
    onBackground = AppColors.OnBackground,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    error = AppColors.Error,
    onError = AppColors.OnError
)

/**
 * Theme for the ChildSafe application
 * Applies MaterialTheme and system UI configuration
 *
 * @param darkTheme Whether to use dark theme based on system settings
 * @param dynamicColor Whether to use Android 12+ dynamic color scheme
 * @param content The content to be themed
 */
@Composable
fun ChildSafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}