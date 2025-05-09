package com.example.childsafe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageStatus
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.viewmodel.MessageViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dialog to show detailed message delivery status information
 * For debugging and support purposes
 */
@Composable
fun MessageStateTrackerDialog(
    conversationId: String,
    onDismiss: () -> Unit,
    messageViewModel: MessageViewModel = hiltViewModel()
) {
    // Collect messages from the viewModel
    val uiState by messageViewModel.uiState.collectAsState()
    val messages = uiState.messages
    val isLoading = uiState.isLoading
    
    // Set conversation when dialog is shown
    LaunchedEffect(conversationId) {
        messageViewModel.setConversation(conversationId)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Dialog title
                Text(
                    text = "Message Delivery Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Message list
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                } else if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages to display",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(messages.filter { it.sender == messageViewModel.getCurrentUserId() }) { message ->
                            MessageStateItem(message)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Individual message status item in the tracker dialog
 */
@Composable
private fun MessageStateItem(message: Message) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Message text preview
        Text(
            text = message.text.take(50) + if (message.text.length > 50) "..." else "",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        
        // Timestamp
        Text(
            text = "Sent: ${dateFormat.format(message.timestamp.toDate())}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Status row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            when (message.deliveryStatus) {
                MessageStatus.SENDING -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Sending",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                MessageStatus.SENT -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                MessageStatus.DELIVERED -> {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Delivered",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                MessageStatus.READ -> {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                MessageStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Status text
            Text(
                text = when (message.deliveryStatus) {
                    MessageStatus.SENDING -> "Sending..."
                    MessageStatus.SENT -> "Sent to server"
                    MessageStatus.DELIVERED -> "Delivered to recipient"
                    MessageStatus.READ -> "Read by recipient"
                    MessageStatus.FAILED -> "Failed to send"
                },
                fontSize = 14.sp,
                color = when (message.deliveryStatus) {
                    MessageStatus.READ -> AppColors.Primary
                    MessageStatus.FAILED -> Color.Red
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        
        // Error message for failed messages
        if (message.deliveryStatus == MessageStatus.FAILED && message.errorMessage.isNotEmpty()) {
            Text(
                text = message.errorMessage,
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Show read receipt details
        if (message.deliveryStatus == MessageStatus.READ && message.readBy.isNotEmpty()) {
            // Currently we just show how many people read it in group chats
            val readByCount = message.readBy.size - 1 // Exclude sender
            if (readByCount > 0) {
                Text(
                    text = "Read by $readByCount ${if (readByCount == 1) "person" else "people"}",
                    fontSize = 12.sp,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
