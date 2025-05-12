package com.example.childsafe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.BuildConfig
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.data.repository.DebugMessagesRepository
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.test.SampleChatData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * ChatViewModel is responsible for managing the list of conversations
 * and providing data for the chat list screen.
 * 
 * This ViewModel:
 * - Fetches and observes conversations from Firestore
 * - Handles creating new conversations
 * - Tracks conversation selection
 * - Provides UI states for loading, error, and success cases
 * - In debug mode, provides sample data for testing
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val debugMessagesRepository: DebugMessagesRepository
) : ViewModel() {

    // UI State for the chat list screen
    data class ChatUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val conversations: List<Conversation> = emptyList(),
        val userChats: UserChats? = null
    )
    
    // MutableStateFlow to hold the current UI state
    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    
    // Public StateFlow for the UI to observe
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // Currently selected conversation ID
    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    init {
        // Load conversations when ViewModel is created
        if (BuildConfig.DEBUG) {
            loadSampleData()
        } else {
            loadConversations()
        }
    }
    
    /**
     * Loads sample data for testing in debug mode
     */
    private fun loadSampleData() {
        Timber.d("Loading sample chat data for debug mode")
        viewModelScope.launch {
            // Simulate loading delay
            delay(500)
            
            try {
                // Check if repository is properly initialized
                val initError = debugMessagesRepository.checkInitialization()
                if (initError != null) {
                    Timber.e("Debug repository initialization error: $initError")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Debug data initialization error: $initError"
                    )
                    return@launch
                }
                
                // Debug direct access to repository
                val repoConversations = debugMessagesRepository.debugConversations.value
                Timber.d("ChatViewModel: Direct repository access shows ${repoConversations.size} conversations")
                
                // Just take the current value to initialize, not an ongoing collection
                val conversations = debugMessagesRepository.debugConversations.value
                
                // Log each conversation for debugging
                Timber.d("ChatViewModel: Loading ${conversations.size} conversations into UI state")
                conversations.forEachIndexed { index, convo ->
                    Timber.d("ChatViewModel: Conversation #${index+1}: id=${convo.id}, lastMsg=${convo.lastMessage?.text ?: "none"}")
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    conversations = conversations,
                    userChats = SampleChatData.testUserChats,
                    errorMessage = null
                )
                
                // Verify the conversations were set correctly
                Timber.d("ChatViewModel: After setting, uiState has ${_uiState.value.conversations.size} conversations")
            } catch (e: Exception) {
                Timber.e(e, "Error loading sample data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load sample data: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Loads conversations for the current user from the repository
     */
    fun loadConversations() {
        Timber.d("ChatViewModel: loadConversations() called, BuildConfig.DEBUG=${BuildConfig.DEBUG}")
        
        // In debug mode, we should load sample data instead
        if (BuildConfig.DEBUG) {
            Timber.d("ChatViewModel: In DEBUG mode, redirecting to loadSampleData()")
            loadSampleData()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Start observing conversations from Firestore
                Timber.d("ChatViewModel: Starting to observe conversations from repository")
                chatRepository.observeConversations().collect { conversations ->
                    Timber.d("ChatViewModel: Received ${conversations.size} conversations from repository")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversations = conversations,
                        errorMessage = null
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load conversations: ${e.localizedMessage}"
                )
            }
        }
        
        // Also load user chats data for unread counts
        viewModelScope.launch {
            try {
                chatRepository.observeUserChats().collect { userChats ->
                    _uiState.value = _uiState.value.copy(
                        userChats = userChats
                    )
                }
            } catch (e: Exception) {
                // Just log the error, don't update UI state since this is secondary data
                Timber.e(e, "Error loading user chats")
            }
        }
    }
    
    /**
     * Creates a new conversation with specified users
     * In debug mode, creates a sample conversation
     */
    fun createConversation(participantIds: List<String>, isGroup: Boolean = false, groupName: String = "") {
        viewModelScope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    // Use the debug repository to create a conversation
                    val conversationId = debugMessagesRepository.createDebugConversation(
                        participantIds,
                        isGroup,
                        groupName
                    )
                    
                    // Select the new conversation
                    _selectedConversationId.value = conversationId
                    Timber.d("Created test conversation with ID: $conversationId")
                } else {
                    val conversationId = chatRepository.createConversation(participantIds, isGroup, groupName)
                    // Select the newly created conversation
                    _selectedConversationId.value = conversationId
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to create conversation: ${e.localizedMessage}"
                )
                Timber.e(e, "Error creating conversation")
            }
        }
    }
    
    /**
     * Selects a conversation to view
     * @param conversationId ID of the selected conversation
     */
    fun selectConversation(conversationId: String) {
        _selectedConversationId.value = conversationId
        
        // Mark messages as read when selecting a conversation
        if (BuildConfig.DEBUG) {
            // Update unread count in debug mode
            val userChats = _uiState.value.userChats
            if (userChats != null) {
                val updatedConversations = userChats.conversations.map { userConvo ->
                    if (userConvo.conversationId == conversationId) {
                        userConvo.copy(unreadCount = 0)
                    } else {
                        userConvo
                    }
                }
                _uiState.value = _uiState.value.copy(
                    userChats = userChats.copy(conversations = updatedConversations)
                )
            }
        } else {
            viewModelScope.launch {
                try {
                    chatRepository.markConversationAsRead(conversationId)
                } catch (e: Exception) {
                    Timber.e(e, "Error marking conversation as read")
                }
            }
        }
    }
    
    /**
     * Clears the currently selected conversation
     */
    fun clearSelectedConversation() {
        _selectedConversationId.value = null
    }
    
    /**
     * Gets unread count for a specific conversation
     * @param conversationId ID of the conversation
     * @return Number of unread messages or 0 if not found
     */
    fun getUnreadCount(conversationId: String): Int {
        val userChats = _uiState.value.userChats ?: return 0
        val conversation = userChats.conversations.find { it.conversationId == conversationId }
        return conversation?.unreadCount ?: 0
    }
    
    /**
     * Clears any error message in the UI state
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Gets messages for a specific conversation (only in debug mode)
     */
    fun getMessagesForConversation(conversationId: String): List<Message> {
        return if (BuildConfig.DEBUG) {
            debugMessagesRepository.getMessagesForConversation(conversationId)
        } else {
            emptyList() // In production, this is handled by MessageViewModel
        }
    }
    
    /**
     * Sends a new message in debug mode
     */
    fun sendDebugMessage(conversationId: String, text: String): Boolean {
        if (!BuildConfig.DEBUG) {
            Timber.w("Attempted to send debug message in production mode")
            return false
        }
        
        if (text.isBlank()) {
            Timber.d("Cannot send empty message")
            return false
        }
        
        Timber.d("Sending debug message to conversation $conversationId: $text")
        
        // Use the debug repository to send the message
        val result = debugMessagesRepository.sendDebugMessage(conversationId, text)
        
        // Update the UI state with the latest conversations from the repository
        viewModelScope.launch {
            try {
                // Just take the current value after sending, no need to collect continuously
                val conversations = debugMessagesRepository.debugConversations.value
                _uiState.value = _uiState.value.copy(conversations = conversations)
                Timber.d("Debug message sent and conversation updated")
            } catch (e: Exception) {
                Timber.e(e, "Error updating conversations after sending message")
            }
        }
        
        return result
    }

    /**
     * For debugging - check the current state and log details
     */
    fun debugState() {
        Timber.d("===== CHAT VIEW MODEL DEBUG STATE =====")
        Timber.d("  UI State isLoading: ${_uiState.value.isLoading}")
        Timber.d("  UI State errorMessage: ${_uiState.value.errorMessage ?: "null"}")
        Timber.d("  UI State conversations count: ${_uiState.value.conversations.size}")
        Timber.d("  UI State conversations IDs: ${_uiState.value.conversations.map { it.id }}")
        Timber.d("  UI State userChats: ${_uiState.value.userChats != null}")
        Timber.d("  Selected conversation ID: ${_selectedConversationId.value ?: "null"}")
        
        // Try direct access to the repository
        val repoConversations = debugMessagesRepository.debugConversations.value
        Timber.d("  Direct repo access conversations count: ${repoConversations.size}")
        Timber.d("  Direct repo access conversation IDs: ${repoConversations.map { it.id }}")
        Timber.d("======================================")
    }

    /**
     * Force refresh conversations directly from the debug repository
     * This is a workaround for cases where the flow collection doesn't trigger properly
     */
    fun forceRefreshConversations() {
        if (BuildConfig.DEBUG) {
            Timber.d("ChatViewModel: Forcing refresh of conversations from debug repository")
            viewModelScope.launch {
                try {
                    // Direct access to repository data
                    val conversations = debugMessagesRepository.debugConversations.value
                    
                    Timber.d("ChatViewModel: Force refresh found ${conversations.size} conversations")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversations = conversations,
                        userChats = SampleChatData.testUserChats,
                        errorMessage = null
                    )
                    
                    // Verify the update worked
                    debugState()
                } catch (e: Exception) {
                    Timber.e(e, "Error during force refresh")
                }
            }
        }
    }
}