package com.example.childsafe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.childsafe.ui.theme.AppColors
import timber.log.Timber

/**
 * SOS Status indicator that shows when an SOS event is active
 * This will appear at the top of the map screen
 */
@Composable
fun SosStatusIndicator(
    isActive: Boolean,
    elapsedTime: Long = 0L,
    onCancelClick: () -> Unit
) {
    // Animation for the pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "sos-pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Format elapsed time
    val formattedTime = formatElapsedTime(elapsedTime)
    
    AnimatedVisibility(
        visible = isActive,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE) // Light red background
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing SOS icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(scale)
                            .background(Color.Red, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS Active",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = "SOS EMERGENCY ACTIVE",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                        
                        Text(
                            text = "Sharing location: $formattedTime",
                            color = Color(0xFF757575),
                            fontSize = 14.sp
                        )
                    }
                    
                    // Cancel button
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel SOS",
                        tint = Color.Gray,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onCancelClick() }
                            .padding(4.dp)
                            .size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CANCEL SOS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Format the elapsed time in minutes and seconds
 */
private fun formatElapsedTime(elapsedTimeMs: Long): String {
    val elapsedSeconds = elapsedTimeMs / 1000
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
