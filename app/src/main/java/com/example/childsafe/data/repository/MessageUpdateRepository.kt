package com.example.childsafe.data.repository

import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageStatus
import com.example.childsafe.validation.MessageValidator
import com.example.childsafe.concurrency.ConflictResolutionService
import com.example.childsafe.concurrency.DocumentVersioningService
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling message updates with validation and conflict resolution
 */
@Singleton
class MessageUpdateRepository @Inject constructor(
    private val documentVersioningService: DocumentVersioningService
) {

    /**
     * Updates a message with proper validation and concurrency control
     *
     * @param messageId The ID of the message to update
     * @param message The original message object
     * @param updates The updates to apply
     * @return The updated message
     */
    suspend fun updateMessageWithConcurrencyControl(
        messageDocRef: DocumentReference,
        message: Message,
        updates: Map<String, Any?>
    ): Message {
        // First validate the updates
        val validationResult = MessageValidator.validateMessage(
            text = updates["text"] as? String ?: message.text,
            messageType = message.getMessageTypeEnum(),
            mediaUrl = updates["mediaUrl"] as? String ?: message.mediaUrl,
            location = message.location
        )
        
        if (!validationResult.isValid()) {
            val errorMessage = validationResult.getFirstErrorOrNull() ?: "Invalid message update"
            Timber.e("Message update validation failed: $errorMessage")
            throw IllegalArgumentException(errorMessage)
        }
        
        // Current version for concurrency control
        val currentVersion = (message.version ?: 0).toLong()
        
        try {
            // Use DocumentVersioningService to handle the update with conflict resolution
            val updatedData = documentVersioningService.updateVersionedDocument(
                messageDocRef,
                updates,
                currentVersion,
                ConflictResolutionService.MapMergeResolver()
            )
            
            // Convert updated data back to a Message object
            val updatedMessage = message.copy(
                text = updatedData["text"] as? String ?: message.text,
                read = updatedData["read"] as? Boolean ?: message.read,
                deliveryStatus = updatedData["deliveryStatus"] as? String ?: message.deliveryStatus,
                version = (updatedData["version"] as? Long)?.toInt() ?: (currentVersion + 1).toInt()
            )
            
            Timber.d("Successfully updated message ${message.id} with concurrency control")
            return updatedMessage
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to update message ${message.id} with concurrency control")
            throw e
        }
    }
    
    /**
     * Updates message delivery status with concurrency control
     *
     * @param messageId The ID of the message
     * @param newStatus The new status to set
     * @param message The original message object
     * @return The updated message
     */
    suspend fun updateMessageStatus(
        messageDocRef: DocumentReference,
        message: Message,
        newStatus: MessageStatus
    ): Message {
        val currentStatus = try {
            MessageStatus.valueOf(message.deliveryStatus)
        } catch (e: IllegalArgumentException) {
            MessageStatus.SENDING
        }
        
        // Only allow valid status transitions
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            Timber.w("Invalid status transition: ${currentStatus.name} -> ${newStatus.name}")
            return message
        }
        
        val updates = mapOf(
            "deliveryStatus" to newStatus.name
        )
        
        return updateMessageWithConcurrencyControl(messageDocRef, message, updates)
    }
    
    /**
     * Mark message as read with concurrency control
     *
     * @param messageId The ID of the message
     * @param userId The ID of the user who read the message
     * @param message The original message object
     * @return The updated message
     */
    suspend fun markMessageAsRead(
        messageDocRef: DocumentReference,
        message: Message,
        userId: String
    ): Message {
        val currentReadBy = message.readBy.toMutableList()
        
        // Don't update if user has already read the message
        if (userId in currentReadBy) {
            return message
        }
        
        // Add user to readBy list
        currentReadBy.add(userId)
        
        val updates = mapOf(
            "read" to true,
            "readBy" to currentReadBy,
            "deliveryStatus" to MessageStatus.READ.name
        )
        
        return updateMessageWithConcurrencyControl(messageDocRef, message, updates)
    }
    
    /**
     * Validates whether a status transition is allowed
     */
    private fun isValidStatusTransition(currentStatus: MessageStatus, newStatus: MessageStatus): Boolean {
        return when (currentStatus) {
            MessageStatus.SENDING -> newStatus in listOf(MessageStatus.SENT, MessageStatus.DELIVERED, MessageStatus.READ)
            MessageStatus.SENT -> newStatus in listOf(MessageStatus.DELIVERED, MessageStatus.READ)
            MessageStatus.DELIVERED -> newStatus == MessageStatus.READ
            MessageStatus.READ -> false // Can't change from READ status
            MessageStatus.FAILED -> TODO()
        }
    }
}
