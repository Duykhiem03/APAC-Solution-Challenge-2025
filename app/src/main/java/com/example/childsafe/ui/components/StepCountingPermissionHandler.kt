package com.example.childsafe.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StepCountingPermissionHandler(
    onPermissionsResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val permissions = buildList {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
        add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions) { permissionResults ->
        val allGranted = permissionResults.all { it.value }
        Timber.d("Step counting permissions result: $permissionResults")
        onPermissionsResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            if (permissionsState.shouldShowRationale) {
                showRationale = true
            } else {
                permissionsState.launchMultiplePermissionRequest()
            }
        } else {
            onPermissionsResult(true)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.step_counter_permission_title)) },
            text = { Text(stringResource(R.string.step_counter_permission_message)) },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    permissionsState.launchMultiplePermissionRequest()
                }) {
                    Text(stringResource(R.string.request_permission))
                }
            },
            dismissButton = {
                Button(onClick = { 
                    showRationale = false
                    onPermissionsResult(false)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = { Text(stringResource(R.string.enable_permissions_in_settings)) },
            confirmButton = {
                Button(onClick = {
                    showSettings = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                Button(onClick = { 
                    showSettings = false
                    onPermissionsResult(false)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
