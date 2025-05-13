package com.example.childsafe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.childsafe.data.model.MessageType

/**
 * Status of an offline message
 */
enum class OfflineMessageStatus {
    PENDING,     // Message is waiting to be sent when connection is restored
    SENDING,     // Message is being sent
    FAILED,      // Message failed to send, will be retried
    SENT,        // Message was successfully sent
    CANCELED     // Message was canceled by user
}

/**
 * Entity for storing messages that were created when offline
 */
@Entity(tableName = "offline_messages")
@TypeConverters(OfflineMessageConverters::class)
data class OfflineMessage(
    @PrimaryKey
    val id: String,                       // Unique ID for the message (generated)
    val conversationId: String,           // ID of the conversation
    val text: String,                     // Message text content
    val messageType: MessageType,         // Type of message (TEXT, IMAGE, etc.)
    val mediaUrl: String? = null,         // URL for media content (if applicable)
    val location: Map<String, Any>? = null, // Location data (if applicable)
    val timestamp: Long,                  // Creation timestamp
    val status: OfflineMessageStatus,     // Current status of the message
    val retryCount: Int = 0,              // Number of retry attempts
    val lastRetryTimestamp: Long = 0,     // Last retry timestamp
    val maxRetries: Int = 3               // Maximum number of retry attempts
)
