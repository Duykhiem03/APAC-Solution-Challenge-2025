package com.example.childsafe.domain.repository

import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.model.UserChats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat functionality
 * Defines methods for interacting with chat data sources
 */
interface ChatRepository {

    /**
     * Observes conversations for the current user
     * @return Flow of conversations list
     */
    suspend fun observeConversations(): Flow<List<Conversation>>
    
    /**
     * Gets a conversation by ID
     * @param conversationId ID of the conversation to retrieve
     * @return The conversation or null if not found
     */
    suspend fun getConversation(conversationId: String): Conversation?
    
    /**
     * Observes messages for a specific conversation
     * @param conversationId ID of the conversation
     * @return Flow of messages list
     */
    suspend fun observeMessages(conversationId: String): Flow<List<Message>>
    
    /**
     * Creates a new conversation
     * @param participantIds IDs of the participants
     * @param isGroup Whether this is a group conversation
     * @param groupName Name for the group (if applicable)
     * @return ID of the new conversation
     */
    suspend fun createConversation(
        participantIds: List<String>,
        isGroup: Boolean = false,
        groupName: String = ""
    ): String
    
    /**
     * Sends a message in a conversation
     * @param conversationId ID of the conversation
     * @param text Text content of the message
     * @param messageType Type of message
     * @param mediaUrl URL for media content (if applicable)
     * @param location Location data (if applicable)
     * @return ID of the sent message
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        messageType: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        location: MessageLocation? = null
    ): String
    
    /**
     * Marks all messages in a conversation as read
     * @param conversationId ID of the conversation
     */
    suspend fun markConversationAsRead(conversationId: String)
    
    /**
     * Observes user chats data containing unread counts
     * @return Flow of UserChats object
     */
    suspend fun observeUserChats(): Flow<UserChats>
    
    /**
     * Deletes a message
     * @param messageId ID of the message to delete
     */
    suspend fun deleteMessage(messageId: String)
    
    /**
     * Deletes an entire conversation
     * @param conversationId ID of the conversation to delete
     */
    suspend fun deleteConversation(conversationId: String)
    
    /**
     * Gets the current user ID
     * @return The current user ID or null if not logged in
     */
    suspend fun getCurrentUserId(): String?
    
    /**
     * Gets older messages before a specified timestamp
     * @param conversationId ID of the conversation
     * @param beforeTimestamp Load messages before this timestamp
     * @param limit Maximum number of messages to load
     * @return List of older messages
     */
    suspend fun getOlderMessages(
        conversationId: String, 
        beforeTimestamp: com.google.firebase.Timestamp, 
        limit: Int = 20
    ): List<Message>
    
    /**
     * Sets the typing status for the current user in a conversation
     * @param conversationId ID of the conversation
     * @param isTyping Whether the user is currently typing
     */
    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean)
    
    /**
     * Observes typing status of users in a conversation
     * @param conversationId ID of the conversation
     * @return Flow of user IDs who are currently typing
     */
    suspend fun observeTypingStatus(conversationId: String): Flow<List<String>>
    
    /**
     * Observes online status of users in a conversation
     * @param conversationId ID of the conversation
     * @return Flow of user IDs who are currently online
     */
    suspend fun observeOnlineStatus(conversationId: String): Flow<List<String>>
}