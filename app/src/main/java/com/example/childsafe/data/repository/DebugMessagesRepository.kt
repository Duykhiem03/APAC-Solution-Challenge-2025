package com.example.childsafe.data.repository

import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.data.model.UserConversation
import com.example.childsafe.test.SampleChatData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing debug messages and conversations
 * This serves as a shared data store for debug mode between different ViewModels
 */
@Singleton
class DebugMessagesRepository @Inject constructor() {
    
    // Store messages for each conversation in debug mode
    private val _debugMessages = MutableStateFlow<MutableMap<String, MutableList<Message>>>(mutableMapOf())
    val debugMessages: StateFlow<Map<String, List<Message>>> = _debugMessages.asStateFlow()
    
    // Debug conversations - Use a more robust approach with direct copy of sample data
    private val _debugConversations = MutableStateFlow<List<Conversation>>(SampleChatData.testConversations)
    val debugConversations: StateFlow<List<Conversation>> = _debugConversations
    
    init {
        try {
            Timber.d("Initializing DebugMessagesRepository")
            loadSampleData()
            Timber.d("DebugMessagesRepository initialization complete with ${_debugConversations.value.size} conversations")
            
            // Extra post-initialization check
            if (_debugConversations.value.isEmpty()) {
                Timber.e("ERROR: DebugMessagesRepository initialized but conversations list is empty!")
            } else {
                Timber.d("DebugMessagesRepository successfully initialized with conversation IDs: " +
                      "${_debugConversations.value.map { it.id }}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in DebugMessagesRepository initialization")
        }
    }
    
    /**
     * Load sample debug data
     */
    private fun loadSampleData() {
        try {
            Timber.d("Loading sample chat data for debug repository")
            
            // Get sample conversations and log details
            val conversations = SampleChatData.testConversations
            Timber.d("Sample conversations loaded: ${conversations.size}")
            conversations.forEach { convo ->
                Timber.d("Conversation: id=${convo.id}, participants=${convo.participants.size}, isGroup=${convo.isGroup}")
            }
            
            // Set sample conversations - store a COPY to avoid reference issues
            _debugConversations.value = conversations.toList()
            
            // Verify the conversations were actually set
            Timber.d("After setting, _debugConversations has ${_debugConversations.value.size} items")
            
            // Initialize debug messages map
            val messagesMap = mutableMapOf<String, MutableList<Message>>()
            val allMessageKeys = SampleChatData.testConversationMessages.keys
            Timber.d("Available message keys: $allMessageKeys")
            
            SampleChatData.testConversations.forEach { conversation ->
                // Get messages from testConversationMessages or initialize an empty list
                val messages = SampleChatData.testConversationMessages[conversation.id]?.toMutableList() ?: mutableListOf()
                Timber.d("Messages for conversation ${conversation.id}: ${messages.size}")
                messagesMap[conversation.id] = messages
            }
            _debugMessages.value = messagesMap
            
            Timber.d("Debug repository loaded with ${messagesMap.size} conversations")
        } catch (e: Exception) {
            Timber.e(e, "Error loading sample data in debug repository")
        }
    }
    
    /**
     * Get messages for a specific conversation
     * @param conversationId ID of the conversation
     * @return List of messages or empty list if conversation not found
     */
    fun getMessagesForConversation(conversationId: String): List<Message> {
        return _debugMessages.value[conversationId] ?: emptyList()
    }
    
    /**
     * Send a debug message in a conversation
     * @param conversationId ID of the conversation
     * @param text Message text
     * @return true if message was sent, false otherwise
     */
    fun sendDebugMessage(conversationId: String, text: String): Boolean {
        try {
            // Create new message with current user as sender
            val newMessage = SampleChatData.createNewMessage(
                conversationId = conversationId,
                text = text,
                sender = "current-user"
            )
            
            // Add message to debug messages
            val messagesMap = _debugMessages.value
            val conversationMessages = messagesMap[conversationId]?.toMutableList() ?: mutableListOf()
            conversationMessages.add(newMessage)
            messagesMap[conversationId] = conversationMessages
            _debugMessages.value = messagesMap
            
            // Update conversation last message
            val currentConversations = _debugConversations.value.toMutableList()
            val conversationIndex = currentConversations.indexOfFirst { it.id == conversationId }
            
            if (conversationIndex >= 0) {
                val conversation = currentConversations[conversationIndex]
                val lastMessage = com.example.childsafe.data.model.LastMessage(
                    text = text,
                    sender = "current-user",
                    timestamp = com.google.firebase.Timestamp.now(),
                    read = true
                )
                
                // Update conversation with new last message
                currentConversations[conversationIndex] = conversation.copy(
                    lastMessage = lastMessage,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                // Sort conversations by updatedAt time (newest first)
                val sortedConversations = currentConversations.sortedByDescending { it.updatedAt }
                
                _debugConversations.value = sortedConversations
            }
            
            // Add auto-response after a short delay for testing
            addDelayedResponse(conversationId, text)
            
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error sending debug message")
            return false
        }
    }
    
    /**
     * Adds a simulated response from the other user after a short delay
     * For testing purposes only
     */
    private fun addDelayedResponse(conversationId: String, originalText: String) {
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(2000) // 2 seconds delay
            
            // Create a response message from other participant
            val participants = _debugConversations.value.find { it.id == conversationId }?.participants
            val otherParticipant = participants?.firstOrNull { it != "current-user" } ?: "test-user-1"
            
            val responseMessage = SampleChatData.createNewMessage(
                conversationId = conversationId,
                text = "This is an auto-response to: \"$originalText\"",
                sender = otherParticipant
            )
            
            // Add response to debug messages
            val updatedMap = _debugMessages.value
            val updatedMessages = updatedMap[conversationId]?.toMutableList() ?: mutableListOf()
            updatedMessages.add(responseMessage)
            updatedMap[conversationId] = updatedMessages
            _debugMessages.value = updatedMap
            
            // Update conversation last message
            val updatedConversations = _debugConversations.value.toMutableList()
            val updatedIndex = updatedConversations.indexOfFirst { it.id == conversationId }
            
            if (updatedIndex >= 0) {
                val conversation = updatedConversations[updatedIndex]
                val lastMessage = com.example.childsafe.data.model.LastMessage(
                    text = responseMessage.text,
                    sender = otherParticipant,
                    timestamp = com.google.firebase.Timestamp.now(),
                    read = false
                )
                
                // Update conversation with new last message
                updatedConversations[updatedIndex] = conversation.copy(
                    lastMessage = lastMessage,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                // Sort conversations by updatedAt time (newest first)
                val sortedConversations = updatedConversations.sortedByDescending { it.updatedAt }
                
                _debugConversations.value = sortedConversations
            }
        }
    }
    
    /**
     * Create a new debug conversation
     */
    fun createDebugConversation(participantIds: List<String>, isGroup: Boolean = false, groupName: String = ""): String {
        val conversationId = "test-convo-" + java.util.UUID.randomUUID().toString().substring(0, 8)
        val newConversation = Conversation(
            id = conversationId,
            participants = listOf("current-user") + participantIds,
            isGroup = isGroup,
            groupName = groupName.ifEmpty() { if (isGroup) "New Group" else "" }
        )
        
        // Add to conversations list
        val currentConversations = _debugConversations.value.toMutableList()
        currentConversations.add(0, newConversation)
        _debugConversations.value = currentConversations
        
        // Initialize empty messages list for new conversation
        val messagesMap = _debugMessages.value
        messagesMap[conversationId] = mutableListOf()
        _debugMessages.value = messagesMap
        
        Timber.d("Created test conversation with ID: $conversationId")
        return conversationId
    }

    /**
     * Check if the debug repository is properly initialized
     * Returns a descriptive message about the state or null if everything is ok
     */
    fun checkInitialization(): String? {
        try {
            val conversations = _debugConversations.value
            val messages = _debugMessages.value
            
            Timber.d("Checking initialization: conversations=${conversations.size}, messagesMap=${messages.size}")
            
            if (conversations.isEmpty()) {
                Timber.e("ERROR: No conversations loaded in debug repository")
                return "No conversations loaded in debug repository"
            }
            
            if (messages.isEmpty()) {
                Timber.e("ERROR: No messages loaded in debug repository")
                return "No messages loaded in debug repository"
            }
            
            // Check that we have messages for at least one conversation
            var hasConversationWithMessages = false
            conversations.forEach { convo ->
                val convoMessages = messages[convo.id]
                Timber.d("Conversation ${convo.id}: has ${convoMessages?.size ?: 0} messages")
                if (messages[convo.id]?.isNotEmpty() == true) {
                    hasConversationWithMessages = true
                }
            }
            
            if (!hasConversationWithMessages) {
                Timber.e("ERROR: No conversation has any messages")
                return "No conversation has any messages"
            }
            
            // Log successful initialization
            Timber.d("Debug repository initialization successful with ${conversations.size} conversations")
            conversations.forEachIndexed { index, convo ->
                Timber.d("Conversation #${index+1}: id=${convo.id}, lastMsg=${convo.lastMessage?.text ?: "none"}")
            }
            
            // Everything seems ok
            return null
        } catch (e: Exception) {
            Timber.e(e, "Error checking initialization")
            return "Error checking initialization: ${e.message}"
        }
    }

    /**
     * Create a UserChats object from a list of conversations
     * @param conversations List of conversations to convert
     * @return UserChats object representing the conversations for the current user
     */
    fun createUserChats(conversations: List<Conversation>): UserChats {
        return UserChats(
            userId = "current-user",
            conversations = conversations.map { conversation ->
                // Count unread messages
                val unreadCount = _debugMessages.value[conversation.id]?.count { 
                    it.sender != "current-user" && it.read != true 
                } ?: 0
                
                UserConversation(
                    conversationId = conversation.id,
                    unreadCount = unreadCount
                )
            }
        )
    }
    
    /**
     * Observe messages for a specific conversation
     * @param conversationId ID of the conversation to observe
     * @return Flow of messages for the conversation
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> {
        return _debugMessages.map { messagesMap ->
            messagesMap[conversationId] ?: emptyList()
        }
    }
}
