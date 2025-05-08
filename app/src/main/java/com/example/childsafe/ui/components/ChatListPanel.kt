package com.example.childsafe.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.ui.components.toDp
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.AppDimensions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * A draggable panel that displays a list of chat conversations
 * The panel height can be adjusted by the user through dragging
 * 
 * @param conversations List of conversations to display
 * @param userChats UserChats data containing unread message counts
 * @param onConversationSelected Callback when a conversation is selected
 * @param onClose Callback when the panel should be closed
 * @param isLoading Whether the conversations are still loading
 */
@Composable
fun ChatListPanel(
    modifier: Modifier = Modifier,
    conversations: List<Conversation>,
    userChats: UserChats? = null,
    onConversationSelected: (String) -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean = false
) {
    // Define the panel height states
    val localDensity = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    
    // Define different height states for the panel
    val minHeight = 120.dp
    val initialHeight = screenHeight * 0.4f // 40% of screen initially
    val maxHeight = screenHeight * 0.8f // 80% of screen maximum
    
    // Keep track of the current height
    var currentHeight by remember { mutableStateOf(initialHeight) }
    
    // Animate height changes for smooth transitions
    val animatedHeight by animateDpAsState(
        targetValue = currentHeight,
        label = "panelHeight"
    )
    
    // Define draggable state
    val draggableState = rememberDraggableState { delta ->
        val newHeight = currentHeight - with(localDensity) { delta.toDp() }
        // Constrain the height between min and max values
        currentHeight = newHeight.coerceIn(minHeight, maxHeight)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(10f)
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Drag handle at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = draggableState
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val newHeight = currentHeight - dragAmount.toDp(localDensity)
                                currentHeight = newHeight.coerceIn(minHeight, maxHeight)
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close"
                            )
                        }
                        
                        Text(
                            text = "Conversations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Indicator of panel resizing capability
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            if (currentHeight < maxHeight - 40.dp) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Drag up to expand"
                                )
                            } else if (currentHeight > minHeight + 40.dp) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Drag down to collapse"
                                )
                            }
                        }
                    }
                }
                
                // Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppColors.Primary
                        )
                    } else if (conversations.isEmpty()) {
                        Text(
                            text = "No conversations yet",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    unreadCount = userChats?.unreadCountForConversation(conversation.id) ?: 0,
                                    onClick = { onConversationSelected(conversation.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extension function to convert pixel delta to dp
 */
private fun Float.toDp(density: androidx.compose.ui.unit.Density): Dp {
    return with(density) { this@toDp.dp }
}

/**
 * Single conversation item in the chat list
 */
@Composable
fun ConversationItem(
    conversation: Conversation,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Determine display name based on conversation type
    val displayName = if (conversation.isGroup) {
        conversation.groupName
    } else {
        // For one-on-one chats, we'll use "User" as a placeholder
        // In a real app, you would get the other participant's name from a UserRepository
        // by looking up the non-current user ID from conversation.participants
        "User"
    }
    
    // Get first letter for avatar
    val avatarInitial = displayName.firstOrNull()?.toString() ?: "?"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (placeholder circle for now)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.Secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Conversation details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
                
                Text(
                    text = conversation.lastMessage?.text ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Right side - time and unread count
            Column(
                horizontalAlignment = Alignment.End
            ) {
                conversation.lastMessage?.timestamp?.let { timestamp ->
                    Text(
                        text = dateFormat.format(timestamp.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper extension function to get unread count for a conversation
 */
fun UserChats.unreadCountForConversation(conversationId: String): Int {
    return conversations.find { it.conversationId == conversationId }?.unreadCount ?: 0
}