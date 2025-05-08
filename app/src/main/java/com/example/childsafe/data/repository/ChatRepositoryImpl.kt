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
    
    // Mock user ID for development testing when auth fails
    private val mockUserId = "mock_user_123456"

    /**
     * Observes conversations for the current user as a Flow
     */
    override suspend fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        // Get current user ID
        val currentUserId = getCurrentUserId()
        
        // Set up listener based on auth state
        val listenerRegistration = if (currentUserId != null) {
            conversationsCollection
                .whereArrayContains("participants", currentUserId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val conversations = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                    } ?: emptyList()

                    trySend(conversations)
                }
        } else {
            // User not logged in - send empty list but don't return early
            Timber.w("User not logged in, returning empty conversations")
            trySend(emptyList())
            null // No actual listener to return
        }

        // This MUST be the final statement in callbackFlow
        awaitClose { 
            listenerRegistration?.remove() 
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
     */
    override suspend fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        
        val listenerRegistration = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        // This MUST be the final statement in callbackFlow
        awaitClose { 
            listenerRegistration.remove() 
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

        // Create message document
        val message = hashMapOf(
            "conversationId" to conversationId,
            "sender" to currentUserId,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false,
            "readBy" to listOf(currentUserId),
            "messageType" to messageType.toString()
        )

        // Add type-specific fields
        when (messageType) {
            MessageType.IMAGE, MessageType.AUDIO -> {
                message["mediaUrl"] = mediaUrl ?: ""
            }
            MessageType.LOCATION -> {
                location?.let {
                    message["locaion"] = mapOf(
                        "latitude" to it.latitude,
                        "longitude" to it.longitude,
                        "locationName" to it.locationName
                    ) as Any
                }
            }
            else -> {} // No additional fields for text messages
        }

        // Add message to Firestore
        val messageRef = messagesCollection.add(message).await()

        // Update conversation's lastMessage
        val lastMessage = hashMapOf(
            "text" to text,
            "sender" to currentUserId,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )

        conversationsCollection.document(conversationId).update(
            mapOf(
                "lastMessage" to lastMessage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

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
                    
                    userChatsCollection.document(participantId).update(
                        "conversations", updatedConversations
                    ).await()
                }
            }
        }

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
    override suspend fun observeUserChats(): Flow<UserChats> = callbackFlow {
        val currentUserId = getCurrentUserId()
        
        // Set up listener based on auth state
        val listenerRegistration = if (currentUserId != null) {
            userChatsCollection
                .document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val userChats = if (snapshot?.exists() == true) {
                        snapshot.toObject(UserChats::class.java)?.copy(userId = snapshot.id)
                            ?: UserChats(userId = currentUserId)
                    } else {
                        UserChats(userId = currentUserId)
                    }

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
     */
    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val currentUserId = getCurrentUserId() ?: return
        
        try {
            // Collection for typing status
            val typingCollection = firestore.collection("typing")
            val typingDocId = "${conversationId}_${currentUserId}"
            
            if (isTyping) {
                // Add or update typing status with automatic expiration (via TTL)
                typingCollection.document(typingDocId).set(
                    hashMapOf(
                        "conversationId" to conversationId,
                        "userId" to currentUserId,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "expiresAt" to Timestamp(System.currentTimeMillis() / 1000 + 10, 0) // 10 seconds TTL
                    )
                ).await()
            } else {
                // Remove typing status
                typingCollection.document(typingDocId).delete().await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating typing status")
        }
    }
    
    /**
     * Observes typing status of users in a conversation
     */
    override suspend fun observeTypingStatus(conversationId: String): Flow<List<String>> = callbackFlow {
        val typingCollection = firestore.collection("typing")
        
        val listenerRegistration = typingCollection
            .whereEqualTo("conversationId", conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Send empty list on error instead of closing
                    Timber.e(error, "Error observing typing status")
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
                
                trySend(typingUsers)
            }
        
        // This MUST be the final statement in callbackFlow
        awaitClose { 
            listenerRegistration.remove() 
        }
    }
    
    /**
     * Observes online status of users in a conversation
     */
    override suspend fun observeOnlineStatus(conversationId: String): Flow<List<String>> = callbackFlow {
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
        
        // Collection for online status
        val onlineCollection = firestore.collection("userStatus")
        
        // We'll observe all participants' online status
        val listenerRegistration = onlineCollection
            .whereIn("userId", conversation.participants)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Send empty list on error instead of closing
                    Timber.e(error, "Error observing online status")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Current timestamp for filtering recently online users (last 2 minutes)
                val twoMinutesAgo = System.currentTimeMillis() / 1000 - 120
                
                // Extract user IDs of users who are online
                val onlineUsers = snapshot?.documents
                    ?.filter { doc -> 
                        val lastActiveTimestamp = doc.getTimestamp("lastActive")
                        lastActiveTimestamp != null && lastActiveTimestamp.seconds > twoMinutesAgo
                    }
                    ?.mapNotNull { it.getString("userId") }
                    ?: emptyList()
                
                trySend(onlineUsers)
            }
        
        // This MUST be the final statement in callbackFlow
        awaitClose { 
            listenerRegistration.remove() 
        }
    }
}