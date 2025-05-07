package com.example.childsafe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
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
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
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
        loadConversations()
    }

    /**
     * Loads conversations for the current user from the repository
     */
    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Start observing conversations from Firestore
                chatRepository.observeConversations().collect { conversations ->
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
                // Log.e("ChatViewModel", "Error loading user chats", e)
            }
        }
    }
    
    /**
     * Creates a new conversation with specified users
     * @param participantIds List of user IDs to include in the conversation
     * @param isGroup Whether the conversation is a group chat
     * @param groupName Name for the group chat (if applicable)
     */
    fun createConversation(participantIds: List<String>, isGroup: Boolean = false, groupName: String = "") {
        viewModelScope.launch {
            try {
                val conversationId = chatRepository.createConversation(participantIds, isGroup, groupName)
                // Select the newly created conversation
                _selectedConversationId.value = conversationId
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to create conversation: ${e.localizedMessage}"
                )
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
        viewModelScope.launch {
            try {
                chatRepository.markConversationAsRead(conversationId)
            } catch (e: Exception) {
                // Just log the error, don't update UI state
                // Log.e("ChatViewModel", "Error marking conversation as read", e)
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
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}