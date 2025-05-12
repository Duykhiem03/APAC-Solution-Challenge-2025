package com.example.childsafe.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data models for Chat Feature
 * These models correspond to the Firestore schema defined in chatDataSchema.js
 */

/**
 * Represents a conversation between users
 */
data class Conversation(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val createdAt: Timestamp? = Timestamp.now(),
    val updatedAt: Timestamp? = Timestamp.now(),
    val lastMessage: LastMessage? = null,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupAdmin: String = "",
    val version: Int? = 1, // For concurrency control
    
    // Online status properties (transient, not stored in Firestore)
    @Transient val onlineParticipants: List<String> = emptyList(),
    @Transient val isParticipantOnline: Boolean? = null,
    @Transient val lastSeenTimestamp: Timestamp? = null
)

/**
 * Represents the last message in a conversation (embedded in Conversation)
 */
data class LastMessage(
    val text: String = "",
    val sender: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false
)

/**
 * Enum for message delivery status
 */
enum class MessageStatus {
    @PropertyName("sending")
    SENDING,
    
    @PropertyName("sent")
    SENT,
    
    @PropertyName("delivered")
    DELIVERED,
    
    @PropertyName("read")
    READ,
    
    @PropertyName("failed")
    FAILED
}

/**
 * Represents a single message in a conversation
 */
data class Message(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val readBy: List<String> = emptyList(),
    val messageType: String = MessageType.TEXT.toString(),
    val mediaUrl: String? = null,
    val location: MessageLocation? = null,
    val deliveryStatus: String = MessageStatus.SENT.toString(),
    val errorMessage: String? = null, // For failed messages
    val version: Int? = 1, // For concurrency control
    val clientTimestamp: Long? = null // For debugging and conflict resolution
) {
    /**
     * Convert string message type to enum
     */
    fun getMessageTypeEnum(): MessageType {
        return try {
            MessageType.valueOf(messageType)
        } catch (e: IllegalArgumentException) {
            MessageType.TEXT
        }
    }
    
    /**
     * Convert string delivery status to enum
     */
    fun getDeliveryStatusEnum(): MessageStatus {
        return try {
            MessageStatus.valueOf(deliveryStatus)
        } catch (e: IllegalArgumentException) {
            MessageStatus.SENDING
        }
    }
}

/**
 * Enum for message types
 */
enum class MessageType {
    @PropertyName("text")
    TEXT,
    
    @PropertyName("image")
    IMAGE,
    
    @PropertyName("location")
    LOCATION,
    
    @PropertyName("audio")
    AUDIO
}

/**
 * Location data for location sharing messages
 */
data class MessageLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = ""
) {
    fun toLatLng(): com.google.android.gms.maps.model.LatLng = 
        com.google.android.gms.maps.model.LatLng(latitude, longitude)
}


// IMPLEMENTATION NOTES:
// 1. These models should be used with FirebaseFirestore to read/write chat data
// 2. Consider adding extension functions for common operations:
//    - Converting to/from Firestore documents
//    - Helper methods for checking if user is participant
//    - Sorting functions for conversations and messages
// 3. Use the @PropertyName annotation for fields that need alternate names in Firestore
// 4. Remember to handle timestamps appropriately for UI display