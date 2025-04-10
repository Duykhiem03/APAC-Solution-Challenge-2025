package com.example.childsafe.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.childsafe.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Handles requesting location permissions for the app
 * Shows appropriate UI based on permission state
 *
 * @param onPermissionResult Callback that provides the permission status
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var permissionChecked by remember { mutableStateOf(false) }
    
    // First check if permission is already granted through system settings
    val isInitialPermissionGranted = remember {
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Immediately inform if permission is already granted on first composition
    LaunchedEffect(Unit) {
        if (isInitialPermissionGranted) {
            Timber.d("LocationPermissionHandler: Permission is already granted through system settings")
            onPermissionResult(true)
            permissionChecked = true
        }
    }

    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    ) { isGranted ->
        Timber.d("LocationPermissionHandler: Permission callback triggered, granted = $isGranted")
        onPermissionResult(isGranted)
        permissionChecked = true
    }

    LaunchedEffect(Unit) {
        // Only check permission status if we haven't already determined it's granted
        if (!isInitialPermissionGranted) {
            when (val status = locationPermissionState.status) {
                is PermissionStatus.Granted -> {
                    Timber.d("LocationPermissionHandler: Permission is granted via permission state")
                    onPermissionResult(true)
                    permissionChecked = true
                }
                is PermissionStatus.Denied -> {
                    val shouldShowRationale = status.shouldShowRationale
                    
                    if (shouldShowRationale) {
                        Timber.d("LocationPermissionHandler: Should show rationale")
                        showRationale = true
                    } else {
                        Timber.d("LocationPermissionHandler: Requesting permission directly")
                        // Small delay to ensure UI is ready
                        delay(100)
                        locationPermissionState.launchPermissionRequest()
                    }
                    
                    // Report initial state as denied unless we've found otherwise
                    if (!permissionChecked) {
                        Timber.d("LocationPermissionHandler: Setting initial permission state to denied")
                        onPermissionResult(false)
                        permissionChecked = true
                    }
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { 
                showRationale = false 
                // When user dismisses rationale without taking action
                if (!permissionChecked) {
                    onPermissionResult(false)
                    permissionChecked = true
                }
            },
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_message)) },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    Timber.d("LocationPermissionHandler: User confirmed permission request after rationale")
                    locationPermissionState.launchPermissionRequest()
                }) {
                    Text(stringResource(R.string.request_permission))
                }
            },
            dismissButton = {
                Button(onClick = {
                    showRationale = false
                    Timber.d("LocationPermissionHandler: User chose to open settings after permission denial")
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        )
    }
}