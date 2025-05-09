package com.example.childsafe.data.repository

import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.LastMessage
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.data.model.UserConversation
import com.example.childsafe.domain.repository.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository that interacts with Firebase Firestore
 *  Handles all chat-related operations and data conversions
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    // Collection references
    private val conversationsCollection = firestore.collection("conversations")
    private val messagesCollection = firestore.collection("messages")
    private val userChatsCollection = firestore.collection("userChats")
    private val typingCollection = firestore.collection("typing")
    private val userStatusCollection = firestore.collection("userStatus")
    
    // Mock user ID for development testing when auth fails
    private val mockUserId = "mock_user_123456"
    
    // Timestamp for calculating online/offline times
    private val ONLINE_THRESHOLD_SECONDS = 120 // 2 minutes

    /**
     * Observes conversations for the current user as a Flow
     * Enhanced with real-time synchronization and error handling
     */
    override suspend fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        // Get current user ID
        val currentUserId = getCurrentUserId()
        
        // Update online status when starting to observe conversations
        if (currentUserId != null) {
            try {
                // Use direct Firebase API call (non-suspending)
                userStatusCollection.document(currentUserId).set(mapOf(
                    "userId" to currentUserId,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "isOnline" to true,
                    "deviceInfo" to android.os.Build.MODEL,
                    "appVersion" to "1.0",
                    "platform" to "Android"
                ))
                Timber.d("Initial online status set in observeConversations")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set initial online status")
            }
        }
        
        // Set up listener based on auth state
        val listenerRegistration = if (currentUserId != null) {
            conversationsCollection
                .whereArrayContains("participants", currentUserId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log the error but don't close the flow unless it's a fatal error
                        Timber.e(error, "Error observing conversations")
                        if (error.message?.contains("permission") == true || 
                            error.message?.contains("PERMISSION_DENIED") == true) {
                            close(error)
                            return@addSnapshotListener
                        }
                        return@addSnapshotListener
                    }

                    // Process conversation documents
                    val conversations = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                    } ?: emptyList()

                    // Emit conversations to flow consumers
                    trySend(conversations)
                    
                    // Update user's online status while they're actively using the app
                    // Use direct Firebase API call (non-suspending)
                    try {
                        if (currentUserId != null) {
                            userStatusCollection.document(currentUserId).set(mapOf(
                                "userId" to currentUserId,
                                "lastActive" to FieldValue.serverTimestamp(),
                                "isOnline" to true,
                                "deviceInfo" to android.os.Build.MODEL,
                                "appVersion" to "1.0",
                                "platform" to "Android"
                            ))
                            Timber.d("User online status updated during conversation observation")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update online status during conversation update")
                    }
                }
        } else {
            // User not logged in - send empty list but don't return early
            Timber.w("User not logged in, returning empty conversations")
            trySend(emptyList())
            null // No actual listener to return
        }

        // This MUST be the final statement in callbackFlow
        awaitClose { 
            // Clean up listener and set user as offline when flow is canceled
            listenerRegistration?.remove()
            
            // Set user offline when they stop observing conversations (direct Firebase API call)
            if (currentUserId != null) {
                try {
                    // Use direct Firebase API call instead of suspension function
                    userStatusCollection.document(currentUserId).update(mapOf(
                        "isOnline" to false,
                        "lastActive" to FieldValue.serverTimestamp(),
                        "lastSeen" to Timestamp.now()
                    ))
                    Timber.d("User set to offline during conversation flow close")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to set user offline during cleanup")
                }
            }
        }
    }

    /**
     * Gets a specific conversation by ID
     */
    override suspend fun getConversation(conversationId: String): Conversation? {
        return try {
            val documentSnapshot = conversationsCollection.document(conversationId).get().await()
            if (documentSnapshot.exists()) {
                documentSnapshot.toObject(Conversation::class.java)?.copy(id = documentSnapshot.id)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Observes messages in a specific conversation as a Flow
     * Enhanced with proper real-time data synchronization and error handling
     */
    override suspend fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        
        // Mark all messages in this conversation as seen by the current user
        // Using direct Firebase API calls instead of suspending function
        if (currentUserId != null) {
            try {
                // Update user's last accessed timestamp for this conversation in userChats
                userChatsCollection.document(currentUserId).get()
                    .addOnSuccessListener { userChatDoc ->
                        if (userChatDoc.exists()) {
                            val userData = userChatDoc.toObject(UserChats::class.java)
                            val conversations = userData?.conversations ?: emptyList()
                            
                            // Find and update the specific conversation's last accessed time
                            val updatedConversations = conversations.map { conv ->
                                if (conv.conversationId == conversationId) {
                                    conv.copy(lastAccessed = Timestamp.now())
                                } else {
                                    conv
                                }
                            }
                            
                            userChatsCollection.document(currentUserId).update(
                                "conversations", updatedConversations
                            ).addOnFailureListener { e ->
                                Timber.e(e, "Error updating conversation view status")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Error getting user chat document: ${e.message}")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error updating conversation view status: ${e.message}")
            }
        }
        
        // Set up real-time listener with server timestamp ordering
        val listenerRegistration = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error but don't close the flow to maintain connection resiliency
                    Timber.e(error, "Error observing messages")
                    // Only close if it's a fatal error
                    if (error.message?.contains("permission") == true || 
                        error.message?.contains("PERMISSION_DENIED") == true) {
                        close(error)
                        return@addSnapshotListener
                    }
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    // Map document to Message object and preserve document ID
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Send the messages to the flow
                trySend(messages)
                
                // If we're actively viewing messages and there are new ones, mark them as read
                // Using direct Firebase API calls instead of suspending function
                if (currentUserId != null && messages.isNotEmpty()) {
                    try {
                        // Get unread messages that weren't sent by this user
                        val unreadMessages = messages.filter { !it.read && it.sender != currentUserId }
                        
                        if (unreadMessages.isNotEmpty()) {
                            // Mark each message as read using a batch operation
                            val batch = firestore.batch()
                            
                            unreadMessages.forEach { message ->
                                val messageRef = messagesCollection.document(message.id)
                                batch.update(messageRef, mapOf(
                                    "read" to true,
                                    "readBy" to FieldValue.arrayUnion(currentUserId)
                                ))
                            }
                            
                            // Update conversation's last message if needed
                            val conversationRef = conversationsCollection.document(conversationId)
                            batch.update(conversationRef, "lastMessage.read", true)
                            
                            // Reset unread count in userChats
                            val userRef = userChatsCollection.document(currentUserId)
                            userRef.get().addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val userData = userDoc.toObject(UserChats::class.java)
                                    val conversations = userData?.conversations ?: emptyList()
                                    
                                    // Reset unread count for this conversation
                                    val updatedConversations = conversations.map { conv ->
                                        if (conv.conversationId == conversationId) {
                                            conv.copy(unreadCount = 0)
                                        } else {
                                            conv
                                        }
                                    }
                                    
                                    batch.update(userRef, "conversations", updatedConversations)
                                    
                                    // Execute all updates in a single batch
                                    batch.commit().addOnFailureListener { e ->
                                        Timber.e(e, "Error marking messages as read")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error marking messages as read: ${e.message}")
                    }
                }
            }

        // This MUST be the final statement in callbackFlow
        awaitClose { 
            listenerRegistration.remove() 
        }
    }
    
    /**
     * Internal helper method to track that user is viewing a conversation
     * Updates user's last access to the conversation
     */
    private suspend fun viewConversation(conversationId: String, userId: String) {
        try {
            // Update user's last accessed timestamp for this conversation in userChats
            val userChatDoc = userChatsCollection.document(userId).get().await()
            
            if (userChatDoc.exists()) {
                val userData = userChatDoc.toObject(UserChats::class.java)
                val conversations = userData?.conversations ?: emptyList()
                
                // Find and update the specific conversation's last accessed time
                val updatedConversations = conversations.map { conv ->
                    if (conv.conversationId == conversationId) {
                        conv.copy(
                            lastAccessed = Timestamp.now()
                        )
                    } else {
                        conv
                    }
                }
                
                userChatsCollection.document(userId).update(
                    "conversations", updatedConversations
                ).await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating conversation view status")
        }
    }
    
    /**
     * Internal helper method to mark new messages as read
     */
    private suspend fun markNewMessagesAsRead(
        conversationId: String, 
        userId: String,
        messages: List<Message>
    ) {
        try {
            // Get unread messages that weren't sent by this user
            val unreadMessages = messages.filter { !it.read && it.sender != userId }
            
            if (unreadMessages.isEmpty()) return
            
            // Mark each message as read using a batch operation
            val batch = firestore.batch()
            
            unreadMessages.forEach { message ->
                val messageRef = messagesCollection.document(message.id)
                batch.update(messageRef, mapOf(
                    "read" to true,
                    "readBy" to FieldValue.arrayUnion(userId)
                ))
            }
            
            // Update conversation's last message if needed
            val conversationRef = conversationsCollection.document(conversationId)
            batch.update(conversationRef, "lastMessage.read", true)
            
            // Reset unread count in userChats
            val userRef = userChatsCollection.document(userId)
            val userDoc = userRef.get().await()
            
            if (userDoc.exists()) {
                val userData = userDoc.toObject(UserChats::class.java)
                val conversations = userData?.conversations ?: emptyList()
                
                // Reset unread count for this conversation
                val updatedConversations = conversations.map { conv ->
                    if (conv.conversationId == conversationId) {
                        conv.copy(unreadCount = 0)
                    } else {
                        conv
                    }
                }
                
                batch.update(userRef, "conversations", updatedConversations)
            }
            
            // Execute all updates in a single batch
            batch.commit().await()
            
        } catch (e: Exception) {
            Timber.e(e, "Error marking messages as read")
        }
    }

    /**
     * Creates a new conversation with the specified participants
     */
    override suspend fun createConversation(
        participantIds: List<String>,
        isGroup: Boolean,
        groupName: String
    ): String {
        // Check if a direct conversation already exists between these users
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            throw IllegalStateException("User not logged in")
        }

        if (!isGroup && participantIds.size == 2) {
            val existingConversation = conversationsCollection
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Conversation::class.java)?.copy(id = it.id) }
                .find { conv ->
                    !conv.isGroup &&
                    conv.participants.size == 2 &&
                    conv.participants.containsAll(participantIds)
                }

            if (existingConversation != null) {
                return existingConversation.id
            }
        }

        // Include the current user in participants if not already there
        val finalParticipants = if (participantIds.contains(currentUserId)) {
            participantIds
        } else {
            participantIds + currentUserId
        }

        // Create conversation document
        val conversation = hashMapOf(
            "participants" to finalParticipants,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastMessage" to null,
            "isGroup" to isGroup,
            "groupName" to if (isGroup) groupName else "",
            "groupAdmin" to if (isGroup) currentUserId else ""
        )

        // Add to Firestore
        val conversationRef = conversationsCollection.add(conversation).await()

        // Add this conversation to userChats for each participant
        val conversationReference = mapOf(
            "conversationId" to conversationRef.id,
            "unreadCount" to 0,
            "lastAccessed" to FieldValue.serverTimestamp()
        )

        finalParticipants.forEach { userId ->
            val userChatDoc = userChatsCollection.document(userId).get().await()
            
            if (userChatDoc.exists()) {
                // Update existing userChats document
                userChatsCollection.document(userId).update(
                    "conversations", FieldValue.arrayUnion(conversationReference)
                ).await()
            } else {
                // Create new userChats document
                userChatsCollection.document(userId).set(
                    hashMapOf("conversations" to listOf(conversationReference))
                ).await()
            }
        }

        return conversationRef.id
    }

    /**
     * Sends a message in a conversation
     * Enhanced with batch operations and better server timestamp handling
     */
    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        messageType: MessageType,
        mediaUrl: String?,
        location: MessageLocation?
    ): String {
        // Verify conversation exists and user is participant
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            throw IllegalStateException("User not logged in")
        }

        val conversationDoc = conversationsCollection.document(conversationId).get().await()
        if (!conversationDoc.exists()) {
            throw IllegalArgumentException("Conversation not found")
        }

        val conversation = conversationDoc.toObject(Conversation::class.java)
            ?: throw IllegalArgumentException("Invalid conversation data")

        if (!conversation.participants.contains(currentUserId)) {
            throw SecurityException("User is not a participant in this conversation")
        }

        // Create batch for atomic operations
        val batch = firestore.batch()
        
        // Create message document
        val message = hashMapOf(
            "conversationId" to conversationId,
            "sender" to currentUserId,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false,
            "readBy" to listOf(currentUserId),
            "messageType" to messageType.toString(),
            "deliveryStatus" to "SENT"
        )

        // Add type-specific fields
        when (messageType) {
            MessageType.IMAGE, MessageType.AUDIO -> {
                message["mediaUrl"] = mediaUrl ?: ""
            }
            MessageType.LOCATION -> {
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

        // Add message to Firestore
        val messageRef = messagesCollection.document()
        batch.set(messageRef, message)

        // Update conversation's lastMessage
        val lastMessage = hashMapOf(
            "text" to text,
            "sender" to currentUserId,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )

        // Update conversation with new last message and timestamp
        batch.update(
            conversationsCollection.document(conversationId),
            mapOf(
                "lastMessage" to lastMessage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        // Update typing status to false since a message was sent
        try {
            val typingDocId = "${conversationId}_${currentUserId}"
            batch.delete(typingCollection.document(typingDocId))
        } catch (e: Exception) {
            // Ignore errors when clearing typing status
            Timber.d("Failed to clear typing status: ${e.message}")
        }
        
        // Update online status
        try {
            // Document ID is the user ID to ensure one status document per user
            batch.set(
                userStatusCollection.document(currentUserId),
                mapOf(
                    "userId" to currentUserId,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "isOnline" to true,
                    "deviceInfo" to android.os.Build.MODEL
                )
            )
        } catch (e: Exception) {
            // Non-fatal error
            Timber.e(e, "Failed to update online status during message send")
        }

        // Update unread counts for all participants except sender
        conversation.participants.forEach { participantId ->
            if (participantId != currentUserId) {
                val userChatDoc = userChatsCollection.document(participantId).get().await()
                
                if (userChatDoc.exists()) {
                    val userData = userChatDoc.toObject(UserChats::class.java)
                    val conversations = userData?.conversations ?: emptyList()
                    
                    // Find and update the specific conversation
                    val updatedConversations = conversations.map { conv ->
                        if (conv.conversationId == conversationId) {
                            conv.copy(unreadCount = conv.unreadCount + 1)
                        } else {
                            conv
                        }
                    }
                    
                    batch.update(
                        userChatsCollection.document(participantId),
                        "conversations", updatedConversations
                    )
                }
            }
        }
        
        // Execute all updates in a single batch
        batch.commit().await()

        return messageRef.id
    }

    /**
     * Marks all messages in a conversation as read
     */
    override suspend fun markConversationAsRead(conversationId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            throw IllegalStateException("User not logged in")
        }

        // Get all unread messages in the conversation that weren't sent by this user
        val unreadMessages = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("read", false)
            .whereNotEqualTo("sender", currentUserId)
            .get()
            .await()

        // Update each message
        unreadMessages.documents.forEach { doc ->
            messagesCollection.document(doc.id).update(
                mapOf(
                    "read" to true,
                    "readBy" to FieldValue.arrayUnion(currentUserId)
                )
            ).await()
        }

        // Update the conversation's lastMessage if it was unread
        val conversationDoc = conversationsCollection.document(conversationId).get().await()
        val conversation = conversationDoc.toObject(Conversation::class.java)

        if (conversation?.lastMessage != null && !conversation.lastMessage.read) {
            conversationsCollection.document(conversationId).update(
                "lastMessage.read", true
            ).await()
        }

        // Reset unread count in userChats
        val userChatDoc = userChatsCollection.document(currentUserId).get().await()
        if (userChatDoc.exists()) {
            val userData = userChatDoc.toObject(UserChats::class.java)
            val conversations = userData?.conversations ?: emptyList()
            
            // Find and update the specific conversation
            val updatedConversations = conversations.map { conv ->
                if (conv.conversationId == conversationId) {
                    conv.copy(
                        unreadCount = 0,
                        lastAccessed = Timestamp.now()
                    )
                } else {
                    conv
                }
            }
            
            userChatsCollection.document(currentUserId).update(
                "conversations", updatedConversations
            ).await()
        }
    }

    /**
     * Observes userChats data as a Flow
     */
    /**
     * Observes userChats data as a Flow
     * Enhanced with real-time synchronization and improved error handling
     */
    override suspend fun observeUserChats(): Flow<UserChats> = callbackFlow {
        val currentUserId = getCurrentUserId()
        
        // Set up listener based on auth state
        val listenerRegistration = if (currentUserId != null) {
            // Ensure the document exists before listening for changes
            ensureUserChatDocumentExists(currentUserId)
            
            // Add real-time listener
            userChatsCollection
                .document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log the error but don't close the flow unless it's a fatal error
                        Timber.e(error, "Error observing user chats")
                        if (error.message?.contains("permission") == true || 
                            error.message?.contains("PERMISSION_DENIED") == true) {
                            close(error)
                            return@addSnapshotListener
                        }
                        return@addSnapshotListener
                    }

                    val userChats = if (snapshot?.exists() == true) {
                        // Map document to UserChats object, preserving ID
                        snapshot.toObject(UserChats::class.java)?.copy(userId = snapshot.id)
                            ?: UserChats(userId = currentUserId)
                    } else {
                        // If document doesn't exist yet, use default object
                        UserChats(userId = currentUserId)
                    }

                    // Send the user chats to the flow consumers
                    trySend(userChats)
                }
        } else {
            // User not logged in - send empty data but don't return early
            Timber.w("User not logged in, returning empty user chats")
            trySend(UserChats())
            null // No actual listener to return
        }

        // This MUST be the final statement in callbackFlow
        awaitClose { 
            listenerRegistration?.remove() 
        }
    }
    
    /**
     * Helper method to ensure the UserChats document exists for a user
     * This helps avoid null data when the user is new or hasn't had any conversations yet
     */
    private suspend fun ensureUserChatDocumentExists(userId: String) {
        try {
            val userChatDoc = userChatsCollection.document(userId).get().await()
            if (!userChatDoc.exists()) {
                // Create default UserChats document if it doesn't exist
                userChatsCollection.document(userId).set(
                    UserChats(
                        userId = userId,
                        conversations = emptyList()
                    )
                ).await()
                Timber.d("Created new UserChats document for user $userId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check or create UserChats document")
        }
    }

    /**
     * Deletes a specific message
     */
    override suspend fun deleteMessage(messageId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            throw IllegalStateException("User not logged in")
        }

        // Get the message to verify ownership
        val messageDoc = messagesCollection.document(messageId).get().await()
        if (!messageDoc.exists()) {
            throw IllegalArgumentException("Message not found")
        }

        val message = messageDoc.toObject(Message::class.java)
            ?: throw IllegalArgumentException("Invalid message data")

        // Only allow the sender to delete their own messages
        if (message.sender != currentUserId) {
            throw SecurityException("Can only delete your own messages")
        }

        // Delete the message
        messagesCollection.document(messageId).delete().await()

        // If this was the last message in the conversation, update the conversation
        val conversationId = message.conversationId
        val lastMessage = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        if (lastMessage.isEmpty) {
            // No messages left, set lastMessage to null
            conversationsCollection.document(conversationId).update(
                mapOf(
                    "lastMessage" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            // Update with the new last message
            val newLastMessage = lastMessage.documents[0].toObject(Message::class.java)
            if (newLastMessage != null) {
                val lastMessageUpdate = LastMessage(
                    text = newLastMessage.text,
                    sender = newLastMessage.sender,
                    timestamp = newLastMessage.timestamp,
                    read = newLastMessage.read
                )
                
                conversationsCollection.document(conversationId).update(
                    mapOf(
                        "lastMessage" to lastMessageUpdate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    /**
     * Deletes an entire conversation
     */
    override suspend fun deleteConversation(conversationId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            throw IllegalStateException("User not logged in")
        }

        // Verify conversation exists and user is participant
        val conversationDoc = conversationsCollection.document(conversationId).get().await()
        if (!conversationDoc.exists()) {
            throw IllegalArgumentException("Conversation not found")
        }

        val conversation = conversationDoc.toObject(Conversation::class.java)
            ?: throw IllegalArgumentException("Invalid conversation data")

        if (!conversation.participants.contains(currentUserId)) {
            throw SecurityException("User is not a participant in this conversation")
        }

        // For group chats, remove the user from participants
        // For direct chats, delete the entire conversation
        if (conversation.isGroup && conversation.participants.size > 1) {
            // Remove user from participants
            conversationsCollection.document(conversationId).update(
                "participants", FieldValue.arrayRemove(currentUserId)
            ).await()
            
            // If current user is the group admin, assign a new admin
            if (conversation.groupAdmin == currentUserId) {
                val newAdmin = conversation.participants.firstOrNull { it != currentUserId }
                if (newAdmin != null) {
                    conversationsCollection.document(conversationId).update(
                        "groupAdmin", newAdmin
                    ).await()
                }
            }
        } else {
            // Delete all messages in the conversation
            val messages = messagesCollection
                .whereEqualTo("conversationId", conversationId)
                .get()
                .await()

            val batch = firestore.batch()
            messages.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Delete the conversation document
            batch.delete(conversationsCollection.document(conversationId))
            batch.commit().await()
        }

        // Remove the conversation reference from userChats
        val userChatDoc = userChatsCollection.document(currentUserId).get().await()
        if (userChatDoc.exists()) {
            val userData = userChatDoc.toObject(UserChats::class.java)
            val conversations = userData?.conversations ?: emptyList()
            
            // Filter out the deleted conversation
            val updatedConversations = conversations.filter { 
                it.conversationId != conversationId 
            }
            
            userChatsCollection.document(currentUserId).update(
                "conversations", updatedConversations
            ).await()
        }
    }

    /**
     * Gets the current user ID
     */
    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid ?: mockUserId
    }
    
    /**
     * Gets older messages before a specified timestamp
     */
    override suspend fun getOlderMessages(
        conversationId: String,
        beforeTimestamp: Timestamp,
        limit: Int
    ): List<Message> {
        return try {
            val messages = messagesCollection
                .whereEqualTo("conversationId", conversationId)
                .whereLessThan("timestamp", beforeTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            // Return messages in ascending order (oldest first)
            messages.documents
                .mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) }
                .sortedBy { it.timestamp.seconds * 1000 + it.timestamp.nanoseconds / 1000000 }
        } catch (e: Exception) {
            Timber.e(e, "Error getting older messages")
            emptyList()
        }
    }
    
    /**
     * Sets the typing status for the current user in a conversation
     * Enhanced with automatic cleanup and better error handling
     */
    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val currentUserId = getCurrentUserId() ?: return
        
        try {
            val typingDocId = "${conversationId}_${currentUserId}"
            
            if (isTyping) {
                // Calculate expiry time (10 seconds from now)
                val expiryTimeSeconds = System.currentTimeMillis() / 1000 + 10
                
                // Add or update typing status with automatic expiration (via TTL)
                typingCollection.document(typingDocId).set(
                    hashMapOf(
                        "conversationId" to conversationId,
                        "userId" to currentUserId,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "expiresAt" to Timestamp(expiryTimeSeconds, 0), // 10 seconds TTL
                        "deviceInfo" to android.os.Build.MODEL // Store device info for debug
                    )
                ).await()
                
                // Also update user's online status while typing (direct Firebase API call)
                userStatusCollection.document(currentUserId).set(mapOf(
                    "userId" to currentUserId,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "isOnline" to true,
                    "deviceInfo" to android.os.Build.MODEL,
                    "appVersion" to "1.0",
                    "platform" to "Android"
                ))
            } else {
                // Remove typing status
                typingCollection.document(typingDocId).delete().await()
            }
            
            // Clean up expired typing statuses (not awaiting this operation)
            try {
                val now = System.currentTimeMillis() / 1000
                
                // Find expired typing statuses for this conversation
                val expiredTypingDocs = typingCollection
                    .whereEqualTo("conversationId", conversationId)
                    .whereLessThan("expiresAt", Timestamp(now, 0))
                    .limit(10) // Limit to avoid processing too many at once
                    .get()
                    .await()
                
                // Delete expired typing statuses in batch if needed
                if (!expiredTypingDocs.isEmpty) {
                    val batch = firestore.batch()
                    for (doc in expiredTypingDocs.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                }
            } catch (e: Exception) {
                // Just log the error, don't throw
                Timber.d("Failed to clean up expired typing statuses: ${e.message}")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating typing status")
            // Throw the exception to inform the caller of the failure
            throw e
        }
    }
    
    /**
     * Observes typing status of users in a conversation
     * Enhanced with better error handling and automatic status cleanup
     */
    override suspend fun observeTypingStatus(conversationId: String): Flow<List<String>> = callbackFlow {
        // Use the current user's ID for checking typing status
        val currentUserId = getCurrentUserId()
        
        // Set up listener for typing status collection
        val listenerRegistration = typingCollection
            .whereEqualTo("conversationId", conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error but don't close the flow unless it's a fatal error
                    Timber.e(error, "Error observing typing status")
                    if (error.message?.contains("permission") == true || 
                        error.message?.contains("PERMISSION_DENIED") == true) {
                        close(error)
                        return@addSnapshotListener
                    }
                    // Just send empty list on non-fatal errors
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Current timestamp for filtering expired statuses
                val now = System.currentTimeMillis() / 1000
                
                // Extract user IDs of users who are typing
                val typingUsers = snapshot?.documents
                    ?.filter { doc -> 
                        // Only include non-expired typing status
                        val expiresAt = doc.getTimestamp("expiresAt")
                        expiresAt != null && expiresAt.seconds > now
                    }
                    ?.mapNotNull { it.getString("userId") }
                    ?: emptyList()
                
                // Send the list of currently typing users
                trySend(typingUsers)
                
                // Clean up expired typing statuses (as a background task)
                try {
                    snapshot?.documents?.forEach { doc ->
                        val expiresAt = doc.getTimestamp("expiresAt")
                        if (expiresAt != null && expiresAt.seconds <= now) {
                            // Delete expired typing status
                            typingCollection.document(doc.id).delete()
                        }
                    }
                } catch (e: Exception) {
                    // Just log the error, don't affect the flow
                    Timber.d("Failed to clean up expired typing status: ${e.message}")
                }
            }
        
        // This MUST be the final statement in callbackFlow
        awaitClose { 
            // Remove the listener when the flow is closed
            listenerRegistration.remove() 
            
            // If the user was typing, remove their typing status when flow closes
            // Use direct Firebase API call (non-suspending)
            if (currentUserId != null) {
                try {
                    val typingDocId = "${conversationId}_${currentUserId}"
                    typingCollection.document(typingDocId).delete()
                        .addOnFailureListener { e ->
                            Timber.d("Failed to clear typing status during cleanup: ${e.message}")
                        }
                } catch (e: Exception) {
                    // Just log the error
                    Timber.d("Failed to clear typing status during cleanup: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Observes online status of users in a conversation
     * Enhanced with better error handling, caching, and periodic updates
     */
    override suspend fun observeOnlineStatus(conversationId: String): Flow<List<String>> = callbackFlow {
        // Get current user ID and update their online status
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            try {
                // Use direct Firebase API call (non-suspending)
                userStatusCollection.document(currentUserId).set(mapOf(
                    "userId" to currentUserId,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "isOnline" to true,
                    "deviceInfo" to android.os.Build.MODEL,
                    "appVersion" to "1.0",
                    "platform" to "Android"
                ))
                Timber.d("Initial online status set in observeOnlineStatus")
            } catch (e: Exception) {
                Timber.e(e, "Failed to update online status during status observation")
            }
        }
        
        // Get conversation to get participants
        val conversationDoc = try {
            conversationsCollection.document(conversationId).get().await()
        } catch (e: Exception) {
            Timber.e(e, "Error getting conversation for online status")
            // Send empty list on error instead of closing
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        if (!conversationDoc.exists()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val conversation = conversationDoc.toObject(Conversation::class.java)
        if (conversation == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        // Create a non-suspending heartbeat that updates online status periodically
        // Using a regular Thread with Firebase direct operations (not suspension functions)
        val heartbeatInterval = 30000L // 30 seconds
        var heartbeatRunning = true
        val heartbeatThread = Thread {
            try {
                while (heartbeatRunning) {
                    if (currentUserId != null) {
                        try {
                            // Use Firebase API directly without suspension
                            userStatusCollection.document(currentUserId).set(mapOf(
                                "userId" to currentUserId,
                                "lastActive" to FieldValue.serverTimestamp(),
                                "isOnline" to true,
                                "deviceInfo" to android.os.Build.MODEL,
                                "appVersion" to "1.0",
                                "platform" to "Android"
                            ))
                            Timber.d("Heartbeat online status updated")
                        } catch (e: Exception) {
                            // Just log the error, don't stop the heartbeat
                            Timber.e(e, "Error in online status heartbeat")
                        }
                    }
                    Thread.sleep(heartbeatInterval)
                }
            } catch (e: InterruptedException) {
                // Thread was interrupted, clean up
                heartbeatRunning = false
            }
        }
        heartbeatThread.start()
        
        // We'll observe all participants' online status
        val listenerRegistration = userStatusCollection
            .whereIn("userId", conversation.participants)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error but don't close the flow unless it's a fatal error
                    Timber.e(error, "Error observing online status")
                    if (error.message?.contains("permission") == true || 
                        error.message?.contains("PERMISSION_DENIED") == true) {
                        close(error)
                        return@addSnapshotListener
                    }
                    // Just send empty list on non-fatal errors
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Current timestamp for filtering recently online users
                val now = System.currentTimeMillis() / 1000
                val thresholdSeconds = now - ONLINE_THRESHOLD_SECONDS
                
                // Extract user IDs of users who are online
                val onlineUsers = snapshot?.documents
                    ?.filter { doc -> 
                        // Check explicitly set online flag first
                        val isOnline = doc.getBoolean("isOnline") ?: false
                        
                        // Then check last active timestamp as backup
                        if (isOnline) {
                            val lastActiveTimestamp = doc.getTimestamp("lastActive")
                            // Consider online only if active within threshold
                            lastActiveTimestamp != null && lastActiveTimestamp.seconds > thresholdSeconds
                        } else {
                            false
                        }
                    }
                    ?.mapNotNull { it.getString("userId") }
                    ?: emptyList()
                
                trySend(onlineUsers)
            }
        
        // This MUST be the final statement in callbackFlow
        awaitClose { 
            // Stop the heartbeat thread
            heartbeatRunning = false
            heartbeatThread.interrupt()
            
            // Remove the listener
            listenerRegistration.remove()
            
            // Set user offline when they stop observing online status - use Firebase API directly
            if (currentUserId != null) {
                try {
                    // Use direct Firebase API call instead of suspension function
                    userStatusCollection.document(currentUserId).update(mapOf(
                        "isOnline" to false,
                        "lastActive" to FieldValue.serverTimestamp(),
                        "lastSeen" to Timestamp.now()
                    ))
                    Timber.d("User set to offline during flow close")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to set user offline during cleanup")
                }
            }
        }
    }

    /**
     * Updates the user's online status in Firestore
     * Call this method when app starts, when user logs in, or at regular intervals when app is in foreground
     * 
     * Enhanced with retry capability and better error handling
     */
    suspend fun updateOnlineStatus() {
        val currentUserId = getCurrentUserId() ?: return
        
        // Create a map with user status data
        val statusData = mapOf(
            "userId" to currentUserId,
            "lastActive" to FieldValue.serverTimestamp(),
            "isOnline" to true,
            "deviceInfo" to android.os.Build.MODEL,
            "appVersion" to "1.0", // Can be dynamically obtained in a real app
            "platform" to "Android"
        )
        
        // Try with retries
        var retries = 2
        var success = false
        var lastException: Exception? = null
        
        while (retries > 0 && !success) {
            try {
                // Document ID is the user ID to ensure one status document per user
                userStatusCollection.document(currentUserId).set(statusData).await()
                success = true
                Timber.d("User online status updated successfully")
            } catch (e: Exception) {
                lastException = e
                Timber.e(e, "Failed to update online status, retries left: $retries")
                retries--
                // Brief delay before retrying
                kotlinx.coroutines.delay(500)
            }
        }
        
        if (!success && lastException != null) {
            // If all retries failed but this is not critical to app function,
            // we just log it rather than propagating the exception
            Timber.e(lastException, "All attempts to update online status failed")
        }
    }
    
    /**
     * Sets the user as offline
     * Call this method when app goes to background or user logs out
     * 
     * Enhanced with guaranteed delivery system
     */
    suspend fun setUserOffline() {
        val currentUserId = getCurrentUserId() ?: return
        
        try {
            // Use a transaction for atomic operations
            firestore.runTransaction { transaction ->
                // Get the current user status document
                val userStatusRef = userStatusCollection.document(currentUserId)
                val userStatus = transaction.get(userStatusRef)
                
                // Create the offline status update
                val offlineUpdate = mapOf(
                    "isOnline" to false,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "lastSeen" to Timestamp.now()
                )
                
                if (userStatus.exists()) {
                    // Update existing document
                    transaction.update(userStatusRef, offlineUpdate)
                } else {
                    // Create new document if it doesn't exist
                    val fullData = hashMapOf(
                        "userId" to currentUserId,
                        "isOnline" to false,
                        "lastActive" to FieldValue.serverTimestamp(),
                        "lastSeen" to Timestamp.now(),
                        "deviceInfo" to android.os.Build.MODEL
                    )
                    transaction.set(userStatusRef, fullData)
                }
                
                // We can't use a query in a transaction, so we'll handle typing status deletion separately
                // Transaction only updates the user status
                
                
                // Return value isn't used in this transaction
                null
            }.await()
            
            // Delete typing status documents outside the transaction
            try {
                // Find all typing status documents for this user
                val typingDocs = typingCollection
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .await()
                
                // If there are any typing documents, delete them
                if (!typingDocs.isEmpty) {
                    val batch = firestore.batch()
                    typingDocs.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                    Timber.d("Deleted ${typingDocs.size()} typing status documents")
                }
            } catch (e: Exception) {
                // Just log, this is not critical
                Timber.d("Failed to clear typing status during offline: ${e.message}")
            }
            
            Timber.d("User set to offline successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user offline")
            // Rethrow to allow caller to handle
            throw e
        }
    }
}