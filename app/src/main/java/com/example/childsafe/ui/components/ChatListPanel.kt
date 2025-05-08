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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

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

    // Tab selection state
    var selectedTab by remember { mutableStateOf(0) }
    
    // Search query state
    var searchQuery by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(10f)
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.PeachBackground, // Using AppColors instead of hardcoded color
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
                            color = AppColors.PeachBackground, // Using AppColors
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
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Close button
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close"
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Indicator of panel resizing capability
                        Box {
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
                
                // Tab buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tab for "Trò chuyện" (Chat)
                    Button(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) AppColors.TabActiveBlue else AppColors.TabInactiveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Trò chuyện",
                            color = if (selectedTab == 0) Color.White else Color.Black
                        )
                    }
                    
                    // Tab for "Bạn bè" (Friends)
                    Button(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) AppColors.TabActiveBlue else AppColors.TabInactiveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Bạn bè",
                            color = if (selectedTab == 1) Color.White else Color.Black
                        )
                    }
                    
                    // Tab for "Kết bạn" (Make friends)
                    Button(
                        onClick = { selectedTab = 2 },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 2) AppColors.TabActiveBlue else AppColors.TabInactiveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Kết bạn",
                            color = if (selectedTab == 2) Color.White else Color.Black
                        )
                    }
                }
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    placeholder = { Text("Tìm kiếm") },
                    leadingIcon = { 
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    shape = RoundedCornerShape(50),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.SearchBarBorder,
                        unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f)
                    )
                )
                
                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // Chat tab content
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
                    1 -> {
                        // Friends tab - you can customize this for your needs
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Friends list will appear here")
                        }
                    }
                    2 -> {
                        // Friend requests tab - you can customize this for your needs
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Friend requests will appear here")
                        }
                    }
                }
                
                // Sample contacts display - shown only when there are no real conversations
                // and we're on the first tab
                if (selectedTab == 0 && conversations.isEmpty() && !isLoading) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Sample data based on the image
                        items(sampleContacts) { contact ->
                            SampleContactItem(
                                name = contact.first,
                                chatNumber = contact.second,
                                onClick = { /* Would navigate to chat but using sample data */ }
                            )
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

// Sample contact data to match the image
private val sampleContacts = listOf(
    "Mẹ" to "Chat #01",
    "Bố" to "Chat #02",
    "Chị hai" to "Chat #03",
    "Bạn thân" to "Chat #04"
)

/**
 * Sample contact item to match the design in the image
 */
@Composable
fun SampleContactItem(
    name: String,
    chatNumber: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder with background color matching the image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppColors.AvatarRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Text(
                text = chatNumber,
                color = AppColors.TextGray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Single conversation item in the chat list - updated to match the image style
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
        "User"
    }
    
    // Get first letter for avatar
    val avatarInitial = displayName.firstOrNull()?.toString() ?: "?"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with the style matching the image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppColors.AvatarRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarInitial,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Text(
                text = conversation.lastMessage?.text ?: "No messages yet",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppColors.TextGray,
                fontSize = 14.sp
            )
        }
        
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AppColors.UnreadBadgeRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
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