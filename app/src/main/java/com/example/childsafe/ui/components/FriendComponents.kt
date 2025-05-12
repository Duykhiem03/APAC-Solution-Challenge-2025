package com.example.childsafe.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.childsafe.data.model.FriendRequest
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.viewmodel.FriendsViewModel
import kotlinx.coroutines.launch

/**
 * Displays a user search result with add friend option
 */
@Composable
fun UserSearchItem(
    user: UserProfile,
    onSendRequest: (String) -> Unit,
    onStartChat: (String) -> Unit,
    friendsViewModel: FriendsViewModel?
) {
    var isFriend by remember { mutableStateOf(false) }
    var hasPendingRequest by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Check friend status
    LaunchedEffect(user.userId) {
        timber.log.Timber.d("UserSearchItem: Checking status for user ${user.userId} (${user.displayName}, ${user.phoneNumber})")
        
        friendsViewModel?.let { vm ->
            // Check if user is already a friend
            isFriend = vm.isFriend(user.userId)
            
            // Check if there's a pending request
            val pendingRequest = vm.getPendingRequestWithUser(user.userId)
            hasPendingRequest = pendingRequest != null
            
            timber.log.Timber.d("UserSearchItem: User ${user.userId} status - isFriend: $isFriend, hasPendingRequest: $hasPendingRequest")
            isLoading = false
        } ?: run {
            timber.log.Timber.w("UserSearchItem: FriendsViewModel is null, cannot check relationship status")
            isLoading = false
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.firstOrNull()?.toString() ?: "U",
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // User info
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            // Action button
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (isFriend) {
                // If they're already a friend, show chat button
                Button(
                    onClick = { 
                        timber.log.Timber.d("UserSearchItem: Chat button clicked for friend ${user.userId} (${user.displayName})")
                        onStartChat(user.userId) 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Chat")
                }
            } else if (hasPendingRequest) {
                // Request pending
                timber.log.Timber.d("UserSearchItem: Showing pending request status for user ${user.userId}")
                Text(
                    text = "Request pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                // Not a friend yet, show add button
                Button(
                    onClick = { 
                        timber.log.Timber.d("UserSearchItem: Add friend button clicked for user ${user.userId} (${user.displayName})")
                        onSendRequest(user.userId) 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Add friend")
                }
            }
        }
    }
}

/**
 * Displays a friend item with options to start chat or remove
 */
@Composable
fun FriendItem(
    friend: UserProfile,
    onStartChat: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onStartChat() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.displayName.firstOrNull()?.toString() ?: "U",
                    color = AppColors.Primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Friend info
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (friend.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (friend.isOnline) Color.Green else Color.Gray
                )
            }
            
            // Action buttons
            Row {
                IconButton(
                    onClick = { onStartChat() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Start chat",
                        tint = AppColors.Primary
                    )
                }
                
                IconButton(
                    onClick = { onRemoveFriend() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove friend",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Displays a received friend request with accept/reject options
 */
@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val senderProfile = request.senderProfile ?: UserProfile(userId = request.senderId, displayName = "Unknown User")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = senderProfile.displayName.firstOrNull()?.toString() ?: "U",
                        color = AppColors.Primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Sender info
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = senderProfile.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = senderProfile.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            
            // Request message if any
            if (request.message.isNotBlank()) {
                Text(
                    text = request.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    ),
                    border = BorderStroke(1.dp, Color.Gray),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Reject")
                }
                
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}

/**
 * Displays a sent friend request with cancel option
 */
@Composable
fun SentRequestItem(
    request: FriendRequest,
    onCancel: () -> Unit
) {
    // We would typically fetch the recipient profile for display
    // For simplicity, showing just the ID here
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            
            // Request info
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = "Request to ${request.recipientId}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Pending response",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                ),
                border = BorderStroke(1.dp, Color.Red),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
