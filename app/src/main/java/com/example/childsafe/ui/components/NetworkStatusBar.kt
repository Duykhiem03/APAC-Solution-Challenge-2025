package com.example.childsafe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A banner that shows network connectivity status
 * Shows when offline and animates when transitioning between states
 */
@Composable
fun NetworkStatusBar(
    isConnected: Boolean,
    hasQueuedMessages: Boolean = false,
    queueSize: Int = 0
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isConnected) Color.Green.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    var previousConnectionState by remember { mutableStateOf(isConnected) }
    var showBar by remember { mutableStateOf(!isConnected) }
    
    // If just connected but we have queued messages, keep showing
    val shouldShowBar = !isConnected || (isConnected && hasQueuedMessages)
    
    LaunchedEffect(isConnected, hasQueuedMessages) {
        // When connectivity changes, update the visibility
        if (previousConnectionState != isConnected) {
            showBar = shouldShowBar
            previousConnectionState = isConnected
        } else if (hasQueuedMessages != showBar) {
            showBar = shouldShowBar
        }
    }
    
    AnimatedVisibility(
        visible = showBar,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Sync else Icons.Default.SignalWifiOff,
                contentDescription = if (isConnected) "Syncing" else "Offline",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected && hasQueuedMessages) {
                    "Syncing $queueSize pending message${if (queueSize > 1) "s" else ""}..."
                } else {
                    "You're offline. Messages will be sent when you reconnect."
                },
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
