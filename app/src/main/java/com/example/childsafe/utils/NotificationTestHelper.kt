package com.example.childsafe.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.LastMessage
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.services.ChatNotificationService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to test notifications from within the app itself.
 * This allows developers to test notification behavior without actually
 * needing to receive a push notification from Firebase Cloud Messaging.
 */
@HiltViewModel
class NotificationTestHelper @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val chatNotificationService: ChatNotificationService
) : ViewModel() {
    
    /**
     * Test function to create a sample notification with the current conversation
     * 
     * @param conversation The current conversation to use for the test
     * @param messageText Text to display in the notification
     * @param messageType Type of message (TEXT, IMAGE, etc.)
     */
    fun testChatMessageNotification(
        conversation: Conversation,
        messageText: String = "This is a test notification",
        messageType: MessageType = MessageType.TEXT
    ) {
        // Create a test message
        val testMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            sender = findOtherParticipant(conversation),
            text = messageText,
            timestamp = Timestamp.now(),
            messageType = messageType,
            read = false
        )
        
        // Get sender name (just use first participant who isn't the current user)
        val senderName = "Test Sender"
        
        // Show the notification
        chatNotificationService.showChatMessageNotification(
            message = testMessage,
            conversation = conversation,
            senderName = senderName
        )
    }
    
    /**
     * Test function to simulate receiving an FCM message payload
     * 
     * @param conversation The current conversation to use for the test
     * @param messageText Text to display in the notification
     * @param messageType Type of message (TEXT, IMAGE, etc.)
     */
    fun testFcmPayload(
        conversation: Conversation,
        messageText: String = "This is a test FCM payload",
        messageType: MessageType = MessageType.TEXT
    ) {
        // Find a sender who isn't the current user
        val senderId = findOtherParticipant(conversation)
        
        // Create a data payload similar to what would be received from FCM
        val data = mapOf(
            "type" to "chat_message",
            "conversationId" to conversation.id,
            "messageId" to UUID.randomUUID().toString(),
            "senderId" to senderId,
            "senderName" to "Test Sender",
            "messageText" to messageText,
            "messageType" to messageType.toString().lowercase(),
            "timestamp" to Date().time.toString(),
            "isGroup" to (conversation.isGroup.toString()),
            "groupName" to conversation.groupName
        )
        
        // Process the test notification payload
        chatNotificationService.handleChatMessageNotification(data)
    }
    
    /**
     * Test function to simulate receiving different types of messages
     * 
     * @param conversation The current conversation to use for the test
     */
    fun testAllMessageTypes(conversation: Conversation) {
        // Queue a text message
        testChatMessageNotification(
            conversation = conversation,
            messageText = "This is a text message test",
            messageType = MessageType.TEXT
        )
        
        // Delay by 2 seconds then send image message
        Thread {
            Thread.sleep(2000)
            testChatMessageNotification(
                conversation = conversation,
                messageText = "Image description", 
                messageType = MessageType.IMAGE
            )
        }.start()
        
        // Delay by 4 seconds then send audio message
        Thread {
            Thread.sleep(4000)
            testChatMessageNotification(
                conversation = conversation,
                messageText = "Voice message", 
                messageType = MessageType.AUDIO
            )
        }.start()
        
        // Delay by 6 seconds then send location message
        Thread {
            Thread.sleep(6000)
            testChatMessageNotification(
                conversation = conversation,
                messageText = "Current location", 
                messageType = MessageType.LOCATION
            )
        }.start()
    }
    
    /**
     * Find a participant in the conversation who isn't the current user
     * to use as a test sender
     */
    private fun findOtherParticipant(conversation: Conversation): String {
        val currentUserId = auth.currentUser?.uid
        
        // Try to find any participant who isn't the current user
        val otherParticipant = conversation.participants.find { it != currentUserId }
        
        // If can't find anyone else, just use a made-up ID
        return otherParticipant ?: "test_sender_id"
    }
}
