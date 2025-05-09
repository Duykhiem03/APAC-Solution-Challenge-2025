package com.example.childsafe.utils

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.repository.ChatRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Utility ViewModel for creating test data in the app
 * This is meant for development and testing purposes
 */
@HiltViewModel
class TestDataHelper @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val chatRepository: ChatRepositoryImpl
) : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    /**
     * Creates a test conversation for the current user with a mock contact
     */
    fun createTestConversation(context: Context) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    _status.value = "Error: User not logged in"
                    Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create a test contact ID (constant so we always use the same one)
                val testContactId = "test_contact_${currentUserId.take(5)}"
                
                // Check if the test user document exists, if not create it
                val testUserDoc = firestore.collection("users").document(testContactId).get().await()
                if (!testUserDoc.exists()) {
                    // Create a test user document
                    firestore.collection("users").document(testContactId).set(
                        hashMapOf(
                            "userId" to testContactId,
                            "displayName" to "Test Contact",
                            "email" to "test.contact@example.com",
                            "photoUrl" to "",
                            "isOnline" to false,
                            "lastActive" to com.google.firebase.Timestamp.now()
                        )
                    ).await()
                }

                // Create a conversation between current user and test contact
                val conversationId = chatRepository.createConversation(
                    participantIds = listOf(currentUserId, testContactId),
                    isGroup = false,
                    groupName = ""
                )

                // Now send a welcome message from the test contact
                val messageId = chatRepository.sendMessage(
                    conversationId = conversationId,
                    text = "👋 Hello! I'm a test contact created for you to test the chat functionality. You can send messages here to test notifications.",
                    messageType = com.example.childsafe.data.model.MessageType.TEXT,
                    mediaUrl = null,
                    location = null,
                    senderId = testContactId // Override sender ID to be the test contact
                )

                // Success message
                _status.value = "Test conversation created successfully! Conversation ID: $conversationId"
                Toast.makeText(context, "Test conversation created!", Toast.LENGTH_SHORT).show()
                
                Timber.d("Created test conversation: $conversationId with message: $messageId")
                
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Timber.e(e, "Failed to create test conversation")
            }
        }
    }

    /**
     * Helper function to extend ChatRepositoryImpl to allow custom sender ID
     * This is for test purposes only
     */
    private suspend fun ChatRepositoryImpl.sendMessage(
        conversationId: String,
        text: String,
        messageType: com.example.childsafe.data.model.MessageType,
        mediaUrl: String? = null,
        location: com.example.childsafe.data.model.MessageLocation? = null,
        senderId: String
    ): String {
        // Create message document manually
        val messageRef = firestore.collection("messages").document()
        
        val message = hashMapOf(
            "conversationId" to conversationId,
            "sender" to senderId,
            "text" to text,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "read" to false,
            "readBy" to listOf(senderId),
            "messageType" to messageType.toString(),
            "deliveryStatus" to "SENT"
        )
        
        // Add any type-specific fields
        when (messageType) {
            com.example.childsafe.data.model.MessageType.IMAGE, 
            com.example.childsafe.data.model.MessageType.AUDIO -> {
                message["mediaUrl"] = mediaUrl ?: ""
            }
            com.example.childsafe.data.model.MessageType.LOCATION -> {
                location?.let {
                    message["location"] = mapOf(
                        "latitude" to it.latitude,
                        "longitude" to it.longitude,
                        "locationName" to it.locationName
                    )
                }
            }
            else -> {} // No additional fields for text messages
        }

        // Add the message document
        messageRef.set(message).await()
        
        // Update conversation's lastMessage
        val lastMessage = hashMapOf(
            "text" to text,
            "sender" to senderId,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "read" to false
        )

        // Update conversation with new last message and timestamp
        firestore.collection("conversations").document(conversationId).update(
            mapOf(
                "lastMessage" to lastMessage,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
        
        // Update unread count for the recipient (current user)
        val userChatDoc = firestore.collection("userChats").document(auth.currentUser!!.uid).get().await()
        if (userChatDoc.exists()) {
            val userData = userChatDoc.toObject(com.example.childsafe.data.model.UserChats::class.java)
            val conversations = userData?.conversations ?: emptyList()
            
            // Update the unread count
            val updatedConversations = conversations.map { conv ->
                if (conv.conversationId == conversationId) {
                    conv.copy(unreadCount = conv.unreadCount + 1)
                } else {
                    conv
                }
            }
            
            firestore.collection("userChats").document(auth.currentUser!!.uid)
                .update("conversations", updatedConversations)
                .await()
        }

        return messageRef.id
    }
}
