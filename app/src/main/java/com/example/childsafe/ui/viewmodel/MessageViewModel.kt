package com.example.childsafe.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.BuildConfig
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.repository.DebugMessagesRepository
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.StorageRepository
import com.example.childsafe.test.SampleChatData
import com.example.childsafe.utils.EventBusManager
import com.example.childsafe.utils.NewMessageEvent
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import java.util.Date

/**
 * MessageViewModel is responsible for managing the messages in an individual conversation.
 *
 * This ViewModel:
 * - Fetches and observes messages for a specific conversation
 * - Handles sending different types of messages (text, image, location, audio)
 * - Manages message input state
 * - Provides UI states for loading, error, and success cases
 * - Handles media uploads for image and audio messages
 * - In debug mode, provides sample data for testing
 */
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageRepository,
    private val debugMessagesRepository: DebugMessagesRepository
) : ViewModel() {

    // UI State for the chat message screen
    data class MessageUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val messages: List<Message> = emptyList(),
        val conversation: Conversation? = null,
        val currentInput: String = "",
        val isAttachmentMenuVisible: Boolean = false,
        val isSendingMessage: Boolean = false,
        val isRecordingAudio: Boolean = false,
        val hasLocationPermission: Boolean = false,
        val isOtherUserTyping: Boolean = false,
        val isOtherUserOnline: Boolean = false,
        val isLoadingOlderMessages: Boolean = false,
        val hasMoreMessagesToLoad: Boolean = true,
        val lastSeenTimestamp: com.google.firebase.Timestamp? = null,
        val isNetworkAvailable: Boolean = true,
        val failedMessages: List<String> = emptyList() // IDs of messages that failed to send
    )

    // MutableStateFlow to hold the current UI state
    private val _uiState = MutableStateFlow(MessageUiState())
    
    // Public StateFlow for the UI to observe
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()
    
    // Current conversation ID
    private val _conversationId = MutableStateFlow<String?>(null)
    
    // Media upload progress
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private var currentConversationId: String? = null
    private var fcmMessageCollector: Job? = null

    // Initialize EventBus listener for status updates
    init {
        // Collect status update events from EventBus
        viewModelScope.launch {
            try {
                EventBusManager.messageStatusFlow.collect { event ->
                    handleStatusUpdate(event.messageId, event.newStatus.toString())
                }
            } catch (e: Exception) {
                Timber.e(e, "Error collecting status updates")
            }
        }

        // Collect user presence events from EventBus
        viewModelScope.launch {
            try {
                EventBusManager.userPresenceFlow.collect { event ->
                    // Only process events for the current conversation
                    if (event.conversationId == null || event.conversationId == _conversationId.value) {
                        handlePresenceUpdate(event)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error collecting presence updates")
            }
        }
    }

    /**
     * Sets the current conversation and loads its messages
     * @param conversationId ID of the conversation to load
     */
    fun setConversation(conversationId: String) {
        if (_conversationId.value == conversationId) return
        
        _conversationId.value = conversationId
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        if (BuildConfig.DEBUG) {
            // In debug mode, load sample conversation data
            loadDebugConversation(conversationId)
        } else {
            // In production, load from repository
            loadConversationFromRepository(conversationId)
        }
    }
    
    /**
     * Loads a debug conversation for testing
     */
    private fun loadDebugConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                // Add small delay to simulate network loading
                delay(300)
                
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
                
                // Get current conversations from debug repository (snapshot)
                val conversations = debugMessagesRepository.debugConversations.value
                val conversation = conversations.find { it.id == conversationId }
                
                // Get messages from debug repository
                val messages = debugMessagesRepository.getMessagesForConversation(conversationId)
                
                if (conversation != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversation = conversation,
                        messages = messages,
                        errorMessage = null
                    )
                    
                    Timber.d("Loaded debug conversation $conversationId with ${messages.size} messages")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Debug conversation not found: $conversationId"
                    )
                    Timber.w("Debug conversation $conversationId not found. Available IDs: ${conversations.map { it.id }}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading conversation: ${e.localizedMessage}"
                )
                Timber.e(e, "Error loading debug conversation $conversationId: ${e.message}")
            }
        }
    }
    
    /**
     * Loads a conversation from the repository in production mode
     */
    private fun loadConversationFromRepository(conversationId: String) {
        // Load conversation details
        viewModelScope.launch {
            try {
                chatRepository.getConversation(conversationId)?.let { conversation ->
                    _uiState.value = _uiState.value.copy(conversation = conversation)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load conversation details: ${e.localizedMessage}"
                )
            }
        }
        
        // Observe messages in real-time
        viewModelScope.launch {
            try {
                chatRepository.observeMessages(conversationId).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages,
                        errorMessage = null
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load messages: ${e.localizedMessage}"
                )
            }
        }
        
        // Mark messages as read when viewing the conversation
        viewModelScope.launch {
            try {
                chatRepository.markConversationAsRead(conversationId)
            } catch (e: Exception) {
                Timber.e(e, "Error marking messages as read")
            }
        }
    }

    /**
     * Updates the current text input value
     * @param input New input text
     */
    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }

    /**
     * Toggles the attachment menu visibility
     */
    fun toggleAttachmentMenu() {
        _uiState.value = _uiState.value.copy(
            isAttachmentMenuVisible = !_uiState.value.isAttachmentMenuVisible
        )
    }

    /**
     * Updates the location permission status
     * @param hasPermission Whether the app has location permission
     */
    fun updateLocationPermission(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
    }

    /**
     * Sends a text message in the current conversation
     * Handles both online and offline scenarios
     * In debug mode, uses the ChatViewModel to send test messages
     */
    fun sendTextMessage() {
        val conversationId = _conversationId.value ?: return
        val text = _uiState.value.currentInput.trim()
        
        if (text.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true, currentInput = "")
        
        if (BuildConfig.DEBUG) {
            // In debug mode, send message through DebugMessagesRepository
            val success = debugMessagesRepository.sendDebugMessage(conversationId, text)
            
            if (success) {
                _uiState.value = _uiState.value.copy(isSendingMessage = false)
                
                // Update local message list
                val messages = debugMessagesRepository.getMessagesForConversation(conversationId)
                _uiState.value = _uiState.value.copy(messages = messages)
                
                Timber.d("Debug message sent: $text")
            } else {
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    errorMessage = "Failed to send test message"
                )
                Timber.w("Failed to send debug message")
            }
            
            return
        }
        
        // Production code path
        viewModelScope.launch {
            try {
                val messageId = chatRepository.sendMessage(conversationId, text, MessageType.TEXT)
                _uiState.value = _uiState.value.copy(isSendingMessage = false)
                
                // If we're offline, show a toast notification
                if (!chatRepository.isOnline()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Message queued. Will send when online."
                    )
                    // Clear error message after a short delay
                    delay(3000)
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    errorMessage = "Failed to send message: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Handles presence update events from EventBus
     */
    private fun handlePresenceUpdate(event: com.example.childsafe.utils.UserPresenceEvent) {
        _uiState.update { state ->
            state.copy(
                isOtherUserOnline = event.isOnline,
                isOtherUserTyping = event.isTyping
            )
        }
    }
    
    /**
     * Handles message status update events from EventBus
     */
    private fun handleStatusUpdate(messageId: String, newStatus: String) {
        // Update the status of a specific message in the UI
        val updatedMessages = _uiState.value.messages.map { message ->
            if (message.id == messageId) {
                message.copy(deliveryStatus = newStatus)
            } else {
                message
            }
        }
        
        _uiState.value = _uiState.value.copy(messages = updatedMessages)
    }

    /**
     * Clear any error messages in the UI state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Update network status in the UI state
     */
    fun updateNetworkStatus(isConnected: Boolean) {
        _uiState.value = _uiState.value.copy(isNetworkAvailable = isConnected)
        Timber.d("Network status updated: $isConnected")
    }

    /**
     * Start observing user status (typing, online) for the current conversation
     * In debug mode, this simulates the other user typing occasionally
     */
    fun startObservingUserStatus() {
        val conversationId = _conversationId.value ?: return
        
        if (BuildConfig.DEBUG) {
            // In debug mode, just set the other user as online
            _uiState.value = _uiState.value.copy(
                isOtherUserOnline = true
            )
            
            // For improved testing experience, simulate the other user typing occasionally
            viewModelScope.launch {
                try {
                    Timber.d("Debug mode: Set other user as online")
                    
                    // Simulate typing indicators periodically for a more realistic experience
                    delay(5000) // Wait 5 seconds after conversation opens
                    _uiState.value = _uiState.value.copy(isOtherUserTyping = true)
                    Timber.d("Debug mode: Other user is typing")
                    
                    delay(3000) // Type for 3 seconds
                    _uiState.value = _uiState.value.copy(isOtherUserTyping = false)
                    Timber.d("Debug mode: Other user stopped typing")
                } catch (e: Exception) {
                    Timber.e(e, "Error in debug user status simulation")
                }
            }
            
            return
        }

        // In production, implement real status observation logic here
        viewModelScope.launch {
            try {
                Timber.d("Starting to observe user status for conversation $conversationId")
                // Implementation would connect to user presence system in non-debug mode
            } catch (e: Exception) {
                Timber.e(e, "Error starting user status observation")
            }
        }
    }

    /**
     * Mark all messages in the current conversation as delivered
     * In debug mode, this only logs the action
     */
    fun markMessagesDelivered() {
        if (BuildConfig.DEBUG) {
            Timber.d("Debug mode: Marking messages as delivered")
            return
        }
        
        // Production implementation would mark messages as delivered
        val conversationId = _conversationId.value ?: return
        
        viewModelScope.launch {
            try {
                // Implementation would update message status
                Timber.d("Marking messages as delivered for conversation $conversationId")
            } catch (e: Exception) {
                Timber.e(e, "Error marking messages as delivered")
            }
        }
    }

    /**
     * Mark a specific message as read
     * In debug mode, this only logs the action
     */
    fun markMessageAsRead(messageId: String) {
        if (BuildConfig.DEBUG) {
            Timber.d("Debug mode: Marking message $messageId as read")
            return
        }
        
        // Production implementation would mark a message as read
        viewModelScope.launch {
            try {
                // Implementation would update message status
                Timber.d("Marking message $messageId as read")
            } catch (e: Exception) {
                Timber.e(e, "Error marking message as read")
            }
        }
    }

    /**
     * Get the current user ID (non-suspending version for UI components)
     * In debug mode, returns "current-user"
     */
    fun getCurrentUserIdSync(): String {
        return if (BuildConfig.DEBUG) {
            "current-user"
        } else {
            // In production, use Firebase Auth directly
            val userId = chatRepository.getCurrentUserIdSync() ?: "unknown-user"
            Timber.d("Sync retrieved user ID: $userId")
            userId
        }
    }

    /**
     * Get the current user ID
     * In debug mode, returns "current-user"
     */
    suspend fun getCurrentUserId(): String {
        return if (BuildConfig.DEBUG) {
            "current-user"
        } else {
            // In production, get the actual user ID
            try {
                chatRepository.getCurrentUserId() ?: "unknown-user"
            } catch (e: Exception) {
                Timber.e(e, "Error getting current user ID")
                "unknown-user"
            }
        }
    }

    /**
     * Start real-time updates for a conversation
     * In debug mode, this only logs the action
     */
    fun startRealtimeUpdates(conversationId: String) {
        if (BuildConfig.DEBUG) {
            Timber.d("Debug mode: Starting real-time updates for conversation $conversationId")
            return
        }
        
        // Production implementation would start real-time updates
        viewModelScope.launch {
            try {
                Timber.d("Starting real-time updates for conversation $conversationId")
            } catch (e: Exception) {
                Timber.e(e, "Error starting real-time updates")
            }
        }
    }

    /**
     * Stop real-time updates for the current conversation
     * In debug mode, this only logs the action
     */
    fun stopRealtimeUpdates() {
        if (BuildConfig.DEBUG) {
            Timber.d("Debug mode: Stopping real-time updates")
            return
        }
        
        // Production implementation would stop real-time updates
        viewModelScope.launch {
            try {
                Timber.d("Stopping real-time updates")
            } catch (e: Exception) {
                Timber.e(e, "Error stopping real-time updates")
            }
        }
    }

    /**
     * Load older messages for the current conversation
     * In debug mode, simulates loading with a delay
     */
    fun loadOlderMessages() {
        val conversationId = _conversationId.value ?: return
        
        _uiState.value = _uiState.value.copy(isLoadingOlderMessages = true)
        
        if (BuildConfig.DEBUG) {
            // In debug mode, just simulate loading with a delay
            viewModelScope.launch {
                Timber.d("Debug mode: Simulating loading older messages")
                delay(1500) // Simulate network delay
                
                // Simulate adding some older messages
                val currentMessages = _uiState.value.messages.toMutableList()
                
                // Only add older messages if we have existing messages
                if (currentMessages.isNotEmpty()) {
                    // Create a few "older" messages
                    val olderMessages = listOf(
                        SampleChatData.createNewMessage(
                            conversationId = conversationId,
                            text = "This is an older message 1",
                            sender = "test-user-1",
                            type = MessageType.TEXT
                        ).copy(timestamp = com.google.firebase.Timestamp(Date(System.currentTimeMillis() - 1000000))),
                        
                        SampleChatData.createNewMessage(
                            conversationId = conversationId,
                            text = "This is an older message 2",
                            sender = "current-user",
                            type = MessageType.TEXT
                        ).copy(timestamp = com.google.firebase.Timestamp(Date(System.currentTimeMillis() - 900000)))
                    )
                    
                    // Add to beginning of the list
                    currentMessages.addAll(0, olderMessages)
                    
                    // Update UI state
                    _uiState.value = _uiState.value.copy(
                        messages = currentMessages,
                        isLoadingOlderMessages = false,
                        // Indicate that there are no more messages to load after this simulation
                        hasMoreMessagesToLoad = false 
                    )
                    
                    // We don't need to update the debug repository separately
                    // since we're now directly updating the UI state only
                    
                    Timber.d("Debug mode: Added ${olderMessages.size} older messages")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingOlderMessages = false,
                        hasMoreMessagesToLoad = false
                    )
                }
            }
            return
        }
        
        // Production implementation
        viewModelScope.launch {
            try {
                // Get the oldest message timestamp to use as reference for loading older messages
                val oldestMessageTimestamp = _uiState.value.messages.minByOrNull { 
                    it.timestamp.seconds * 1000 + it.timestamp.nanoseconds / 1000000 
                }?.timestamp ?: com.google.firebase.Timestamp.now()
                
                // Load older messages from before the oldest message we currently have
                val olderMessages = chatRepository.getOlderMessages(
                    conversationId = conversationId,
                    beforeTimestamp = oldestMessageTimestamp,
                    limit = 20 // Fetch 20 messages at a time
                )
                
                if (olderMessages.isNotEmpty()) {
                    val currentMessages = _uiState.value.messages.toMutableList()
                    currentMessages.addAll(0, olderMessages)
                    
                    _uiState.value = _uiState.value.copy(
                        messages = currentMessages,
                        isLoadingOlderMessages = false,
                        hasMoreMessagesToLoad = olderMessages.size >= 20 // Assuming we fetch 20 at a time
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingOlderMessages = false,
                        hasMoreMessagesToLoad = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingOlderMessages = false,
                    errorMessage = "Failed to load older messages: ${e.localizedMessage}"
                )
                Timber.e(e, "Error loading older messages")
            }
        }
    }

    /**
     * Retry sending a specific failed message
     * @param messageId ID of the failed message to retry
     */
    fun retryMessage(messageId: String) {
        if (BuildConfig.DEBUG) {
            Timber.d("Debug mode: Retrying message $messageId")
            
            // Find the message in our list
            val messages = _uiState.value.messages.toMutableList()
            val messageIndex = messages.indexOfFirst { it.id == messageId }
            
            if (messageIndex >= 0) {
                // Update the message status to SENDING
                val message = messages[messageIndex].copy(deliveryStatus = com.example.childsafe.data.model.MessageStatus.SENDING.toString())
                messages[messageIndex] = message
                
                // Update UI with the message in SENDING state
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    failedMessages = _uiState.value.failedMessages.filter { it != messageId }
                )
                
                // Simulate sending process
                viewModelScope.launch {
                    delay(1500) // Simulate network delay
                    
                    // Update to SENT status
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    val updatedIndex = updatedMessages.indexOfFirst { it.id == messageId }
                    
                    if (updatedIndex >= 0) {
                        val updatedMessage = updatedMessages[updatedIndex].copy(
                            deliveryStatus = com.example.childsafe.data.model.MessageStatus.SENT.toString()
                        )
                        updatedMessages[updatedIndex] = updatedMessage
                        
                        _uiState.value = _uiState.value.copy(messages = updatedMessages)
                        Timber.d("Debug mode: Message $messageId retry successful")
                    }
                }
            }
            return
        }
        
        // Production implementation
        viewModelScope.launch {
            try {
                Timber.d("Retrying message $messageId")
                val success = chatRepository.retryMessage(messageId)
                
                if (success) {
                    // Update the failed messages list
                    _uiState.value = _uiState.value.copy(
                        failedMessages = _uiState.value.failedMessages.filter { it != messageId }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to retry message"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error retrying message: ${e.localizedMessage}"
                )
                Timber.e(e, "Error retrying message $messageId")
            }
        }
    }

    /**
     * Retry all failed messages in the current conversation
     */
    fun forceRetryFailedMessages() {
        val failedMessageIds = _uiState.value.failedMessages
        
        if (failedMessageIds.isEmpty()) {
            return
        }
        
        Timber.d("Retrying ${failedMessageIds.size} failed messages")
        
        if (BuildConfig.DEBUG) {
            // In debug mode, simulate retrying all failed messages
            val messages = _uiState.value.messages.toMutableList()
            
            // Update all failed messages to SENDING
            for (i in messages.indices) {
                if (failedMessageIds.contains(messages[i].id)) {
                    messages[i] = messages[i].copy(deliveryStatus = com.example.childsafe.data.model.MessageStatus.SENDING.toString())
                }
            }
            
            // Update UI with all messages in SENDING state
            _uiState.value = _uiState.value.copy(
                messages = messages,
                failedMessages = emptyList()
            )
            
            // Simulate sending process
            viewModelScope.launch {
                delay(2000) // Simulate network delay
                
                // Update to SENT status
                val updatedMessages = _uiState.value.messages.map { message ->
                    if (failedMessageIds.contains(message.id)) {
                        message.copy(deliveryStatus = com.example.childsafe.data.model.MessageStatus.SENT.toString())
                    } else {
                        message
                    }
                }
                
                _uiState.value = _uiState.value.copy(messages = updatedMessages)
                Timber.d("Debug mode: All ${failedMessageIds.size} message retries successful")
            }
            return
        }
        
        // Production implementation
        viewModelScope.launch {
            try {
                val results = failedMessageIds.map { messageId ->
                    chatRepository.retryMessage(messageId)
                }
                
                val successCount = results.count { it }
                
                if (successCount == failedMessageIds.size) {
                    _uiState.value = _uiState.value.copy(
                        failedMessages = emptyList(),
                        errorMessage = "All messages retried successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Retried $successCount of ${failedMessageIds.size} messages"
                    )
                }
                
                // After a short delay, clear the success message
                delay(2000)
                if (_uiState.value.errorMessage?.contains("retried") == true) {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error retrying messages: ${e.localizedMessage}"
                )
                Timber.e(e, "Error retrying messages")
            }
        }
    }
}