package com.example.childsafe.services

import android.content.Context
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageStatus
import com.example.childsafe.data.model.MessageType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to manage message delivery status updates
 * This service:
 * - Updates message status in Firestore when messages are delivered or read
 * - Registers listeners for status updates from other devices
 * - Calls Firebase Cloud Functions to propagate status changes
 */
@Singleton
class MessageDeliveryService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    
    private val messagesCollection = firestore.collection("messages")
    
    /**
     * Mark a message as delivered (recipient received it)
     * 
     * @param messageId The ID of the message to mark as delivered
     * @return True if successful, false otherwise
     */
    suspend fun markMessageDelivered(messageId: String): Boolean {
        return try {
            // Update locally in Firestore
            val messageRef = messagesCollection.document(messageId)
            val messageDoc = messageRef.get().await()
            
            if (!messageDoc.exists()) {
                Timber.e("Message $messageId not found")
                return false
            }
            
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Timber.e("User not logged in")
                return false
            }
            
            val messageSender = messageDoc.getString("sender")
            
            // Only update if we're not the sender
            if (messageSender != currentUserId) {
                // Update status locally
                val currentStatus = messageDoc.getString("deliveryStatus")
                if (currentStatus == "SENDING" || currentStatus == "SENT") {
                    messageRef.update("deliveryStatus", "DELIVERED").await()
                    
                    // Call Cloud Function to update on other devices
                    val data = hashMapOf(
                        "messageId" to messageId
                    )
                    
                    functions.getHttpsCallable("markMessageDelivered")
                        .call(data)
                        .await()
                }
                
                return true
            }
            
            return false
        } catch (e: Exception) {
            Timber.e(e, "Error marking message as delivered")
            false
        }
    }
    
    /**
     * Mark a message as read (recipient viewed it)
     * 
     * @param messageId The ID of the message to mark as read
     * @return True if successful, false otherwise
     */
    suspend fun markMessageRead(messageId: String): Boolean {
        return try {
            // Update locally in Firestore
            val messageRef = messagesCollection.document(messageId)
            val messageDoc = messageRef.get().await()
            
            if (!messageDoc.exists()) {
                Timber.e("Message $messageId not found")
                return false
            }
            
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Timber.e("User not logged in")
                return false
            }
            
            val messageSender = messageDoc.getString("sender")
            
            // Only update if we're not the sender
            if (messageSender != currentUserId) {
                // Update status locally
                messageRef.update(
                    mapOf(
                        "deliveryStatus" to "READ",
                        "read" to true,
                        "readBy" to com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
                    )
                ).await()
                
                // Call Cloud Function to update on other devices
                val data = hashMapOf(
                    "messageId" to messageId
                )
                
                functions.getHttpsCallable("markMessageRead")
                    .call(data)
                    .await()
                
                return true
            }
            
            return false
        } catch (e: Exception) {
            Timber.e(e, "Error marking message as read")
            false
        }
    }
    
    /**
     * Mark all messages in a conversation as read
     * 
     * @param conversationId The ID of the conversation
     * @return Number of messages marked as read
     */
    suspend fun markConversationMessagesRead(conversationId: String): Int {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Timber.e("User not logged in")
                return 0
            }
            
            Timber.d("Marking conversation messages as read for conversation: $conversationId")
            
            // Call Cloud Function to update all messages
            val data = hashMapOf(
                "conversationId" to conversationId
            )
            
            val result = functions.getHttpsCallable("markConversationMessagesRead")
                .call(data)
                .await()
                .data
            
            // Parse result if available
            if (result is Map<*, *>) {
                val success = (result["success"] as? Boolean) ?: false
                val count = (result["count"] as? Number)?.toInt() ?: 0
                
                if (success) {
                    Timber.d("Successfully marked $count messages as read in conversation: $conversationId")
                } else {
                    val reason = result["reason"] as? String ?: "Unknown reason"
                    Timber.w("Failed to mark messages as read: $reason")
                }
                
                return count
            }
            
            Timber.w("Unexpected result format when marking messages as read")
            return 0
        } catch (e: Exception) {
            // Log detailed error information
            val errorMessage = e.message ?: "Unknown error"
            val errorCause = e.cause?.message ?: "No cause"
            Timber.e(e, "Error marking conversation messages as read: $errorMessage, Cause: $errorCause")
            
            // Rethrow with clearer message for UI display
            throw Exception("Error updating message statuses. Please try again later.", e)
        }
    }
    
    /**
     * Handle a message status update notification from FCM
     * 
     * @param messageId ID of the message to update
     * @param newStatus New status value
     * @return True if status was updated, false otherwise
     */
    suspend fun handleStatusUpdateNotification(messageId: String, newStatus: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val messageRef = messagesCollection.document(messageId)
                val messageDoc = messageRef.get().await()
                
                if (!messageDoc.exists()) {
                    Timber.e("Message $messageId not found in status update")
                    return@withContext false
                }
                
                // Update the status
                when (newStatus.uppercase()) {
                    "SENT" -> messageRef.update("deliveryStatus", "SENT").await()
                    "DELIVERED" -> messageRef.update("deliveryStatus", "DELIVERED").await()
                    "READ" -> messageRef.update(
                        mapOf(
                            "deliveryStatus" to "READ",
                            "read" to true
                        )
                    ).await()
                    else -> {
                        Timber.w("Unknown status update: $newStatus")
                        return@withContext false
                    }
                }
                
                Timber.d("Message $messageId status updated to $newStatus")
                return@withContext true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling status update notification")
            false
        }
    }
    
    /**
     * Update the UI state when a conversation is viewed, marking messages as delivered
     * Call this when the user opens a conversation
     * 
     * @param conversationId The conversation ID being viewed
     */
    suspend fun processNewlyDeliveredMessages(conversationId: String) {
        try {
            withContext(Dispatchers.IO) {
                val currentUserId = auth.currentUser?.uid ?: return@withContext
                
                // Find messages that need to be marked as delivered
                // (Messages not sent by current user and in SENT status)
                val messagesToUpdate = messagesCollection
                    .whereEqualTo("conversationId", conversationId)
                    .whereNotEqualTo("sender", currentUserId)
                    .whereEqualTo("deliveryStatus", "SENT")
                    .get()
                    .await()
                
                // Mark each as delivered
                for (doc in messagesToUpdate.documents) {
                    try {
                        markMessageDelivered(doc.id)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to mark message ${doc.id} as delivered")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing newly delivered messages")
        }
    }

    /**
     * Creates or retrieves a Message object from notification data
     * Used when receiving an FCM notification about a new message
     * 
     * @param messageId ID of the message
     * @param conversationId ID of the conversation
     * @param senderId ID of the sender
     * @param messageType Type of message (text, image, etc.)
     * @param messageText Text content of the message (may be null for non-text types)
     * @param timestamp When the message was sent (milliseconds)
     * @return Message object if successful, null otherwise
     */
    suspend fun getOrCreateMessageFromNotification(
        messageId: String,
        conversationId: String,
        senderId: String,
        messageType: String,
        messageText: String?,
        timestamp: Timestamp
    ): com.example.childsafe.data.model.Message? {
        return try {
            withContext(Dispatchers.IO) {
                // First check if message already exists in local database/cache
                val localMessage = messagesCollection.document(messageId).get().await()
                
                if (localMessage.exists()) {
                    // Message already exists, convert to model object
                    localMessage.toObject(com.example.childsafe.data.model.Message::class.java)
                } else {
                    // Message doesn't exist locally, create a new model object
                    val message = Message(
                        id = messageId,
                        conversationId = conversationId,
                        sender = senderId,
                        text = messageText.toString(),
                        timestamp = timestamp,
                        deliveryStatus = MessageStatus.DELIVERED.name, // Mark as delivered since we received it
                        messageType = MessageType.valueOf(messageType.uppercase())
                            .toString()
                    )
                    
                    // Store in Firestore for future reference
                    messagesCollection.document(messageId).set(message).await()
                    
                    // Mark as delivered instantly
                    markMessageDelivered(messageId)
                    
                    message
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating message from notification")
            null
        }
    }
}
