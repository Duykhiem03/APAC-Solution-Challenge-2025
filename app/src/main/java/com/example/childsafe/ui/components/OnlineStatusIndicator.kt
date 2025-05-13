package com.example.childsafe.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childsafe.ui.theme.AppColors

/**
 * Component that displays a user's online status with an animated indicator
 * 
 * @param isOnline Whether the user is currently online
 * @param lastSeen Optional timestamp of when the user was last seen (for offline status)
 */
@Composable
fun OnlineStatusIndicator(
    isOnline: Boolean,
    lastSeen: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOnline) {
            // Animated pulse effect for online indicator
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(AppColors.GpsActive)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Online",
                fontSize = 12.sp,
                color = AppColors.GpsActive
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = lastSeen ?: "Offline",
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Component that displays a user's online status as a simple dot
 * Used in situations where space is limited
 * 
 * @param isOnline Whether the user is currently online
 * @param modifier Modifier for styling the indicator
 */
@Composable
fun OnlineStatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated pulse effect for online indicator
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOnline) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .size(8.dp)
            .scale(if (isOnline) scale else 1f)
            .clip(CircleShape)
            .background(if (isOnline) AppColors.GpsActive else Color.Gray)
    )
}
