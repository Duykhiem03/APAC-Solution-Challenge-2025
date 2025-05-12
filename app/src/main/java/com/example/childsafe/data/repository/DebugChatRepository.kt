package com.example.childsafe.data.repository

import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.services.MessageDeliveryService
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug implementation of ChatRepository that uses DebugMessagesRepository
 * for providing sample data and debug functionality.
 */
@Singleton
class DebugChatRepository @Inject constructor(
    private val debugMessagesRepository: DebugMessagesRepository,
    override val messageDeliveryService: MessageDeliveryService
) : ChatRepository {

    // State for tracking online status in debug mode
    private val _isOnline = MutableStateFlow(true)

    override fun isOnline(): Boolean = _isOnline.value

    override suspend fun observeConversations(): Flow<List<Conversation>> {
        return debugMessagesRepository.debugConversations
    }

    override suspend fun observeUserChats(): Flow<UserChats> {
        // Convert conversations to UserChats format
        return debugMessagesRepository.debugConversations.map { conversations ->
            val userChats = debugMessagesRepository.createUserChats(conversations)
            userChats
        }
    }

    override suspend fun observeMessages(conversationId: String): Flow<List<Message>> {
        return debugMessagesRepository.observeMessages(conversationId)
    }

    override suspend fun getConversation(conversationId: String): Conversation? {
        return debugMessagesRepository.debugConversations.value.find { it.id == conversationId }
    }

    override suspend fun createConversation(
        participantIds: List<String>,
        isGroup: Boolean,
        groupName: String
    ): String {
        return debugMessagesRepository.createDebugConversation(participantIds, isGroup, groupName)
    }

    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        messageType: MessageType,
        mediaUrl: String?,
        location: MessageLocation?
    ): String {
        val success = debugMessagesRepository.sendDebugMessage(conversationId, text)
        return if (success) {
            val messages = debugMessagesRepository.getMessagesForConversation(conversationId)
            messages.lastOrNull()?.id ?: "unknown-message-id"
        } else {
            throw RuntimeException("Failed to send debug message")
        }
    }

    override suspend fun markConversationAsRead(conversationId: String) {
        Timber.d("Debug: Marking conversation $conversationId as read")
    }

    override suspend fun getOlderMessages(
        conversationId: String,
        beforeTimestamp: Timestamp,
        limit: Int
    ): List<Message> {
        // In debug mode, just return an empty list since we load all messages at once
        return emptyList()
    }
    
    override suspend fun retryFailedMessages() {
        Timber.d("Debug: Retrying all failed messages")
        // In debug mode, we don't actually have failed messages to retry
        // This would just simulate the process
    }

    override suspend fun retryMessage(messageId: String): Boolean {
        Timber.d("Debug: Retrying message $messageId")
        return true
    }

    override fun getCurrentUserIdSync(): String? {
        return "current-user" // Standard debug user ID
    }

    override suspend fun getCurrentUserId(): String? {
        return "current-user" // Standard debug user ID
    }

    override suspend fun deleteMessage(messageId: String) {
        Timber.d("Debug: Deleting message $messageId")
    }

    override suspend fun deleteConversation(conversationId: String) {
        Timber.d("Debug: Deleting conversation $conversationId")
    }

    suspend fun editMessage(messageId: String, newText: String): Boolean {
        Timber.d("Debug: Editing message $messageId to: $newText")
        return true
    }

    suspend fun getMessageById(messageId: String): Message? {
        // Find the message in all conversations
        val allMessages = debugMessagesRepository.debugConversations.value.flatMap { conversation ->
            debugMessagesRepository.getMessagesForConversation(conversation.id)
        }
        return allMessages.find { it.id == messageId }
    }

    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        Timber.d("Debug: Setting typing status for conversation $conversationId to $isTyping")
    }

    override suspend fun observeTypingStatus(conversationId: String): Flow<List<String>> {
        return MutableStateFlow(emptyList<String>())
    }

    override suspend fun observeOnlineStatus(conversationId: String): Flow<List<String>> {
        // Return all participants as online in debug mode
        val conversation = debugMessagesRepository.debugConversations.value.find { it.id == conversationId }
        val participants = conversation?.participants?.filterNot { it == getCurrentUserIdSync() } ?: emptyList()
        return MutableStateFlow(participants)
    }

    // Helper method to simulate going offline/online
    fun setOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
        Timber.d("Debug: Set online status to $isOnline")
    }
}
