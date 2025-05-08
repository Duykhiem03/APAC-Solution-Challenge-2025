package com.example.childsafe.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.StorageRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.Timestamp
import javax.inject.Inject

/**
 * MessageViewModel is responsible for managing the messages in an individual conversation.
 *
 * This ViewModel:
 * - Fetches and observes messages for a specific conversation
 * - Handles sending different types of messages (text, image, location, audio)
 * - Manages message input state
 * - Provides UI states for loading, error, and success cases
 * - Handles media uploads for image and audio messages
 */
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageRepository
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
        val lastSeenTimestamp: Timestamp? = null,
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

    /**
     * Sets the current conversation and loads its messages
     * @param conversationId ID of the conversation to load
     */
    fun setConversation(conversationId: String) {
        if (_conversationId.value == conversationId) return
        
        _conversationId.value = conversationId
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
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
                // Just log the error but don't update UI state
                // Log.e("MessageViewModel", "Error marking messages as read", e)
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
     */
    fun sendTextMessage() {
        val conversationId = _conversationId.value ?: return
        val text = _uiState.value.currentInput.trim()
        
        if (text.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true, currentInput = "")
        
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(conversationId, text, MessageType.TEXT)
                _uiState.value = _uiState.value.copy(isSendingMessage = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    errorMessage = "Failed to send message: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Sends an image message in the current conversation
     * @param imageUri URI of the image to send
     */
    fun sendImageMessage(imageUri: Uri) {
        val conversationId = _conversationId.value ?: return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true)
        _uploadProgress.value = 0f
        
        viewModelScope.launch {
            try {
                // Upload image to storage
                val imageUrl = storageRepository.uploadImage(
                    imageUri,
                    "conversations/$conversationId/images",
                    onProgressChanged = { progress -> _uploadProgress.value = progress }
                )
                
                // Send message with image URL
                chatRepository.sendMessage(
                    conversationId,
                    "Image",
                    MessageType.IMAGE,
                    mediaUrl = imageUrl
                )
                
                _uiState.value = _uiState.value.copy(isSendingMessage = false)
                _uploadProgress.value = null
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    errorMessage = "Failed to send image: ${e.localizedMessage}"
                )
                _uploadProgress.value = null
            }
        }
    }

    /**
     * Sends a location message in the current conversation
     * @param latLng LatLng object containing the location coordinates
     * @param locationName Optional name of the location
     */
    fun sendLocationMessage(latLng: LatLng, locationName: String = "") {
        val conversationId = _conversationId.value ?: return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true)
        
        viewModelScope.launch {
            try {
                val location = MessageLocation(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    locationName = locationName
                )
                
                // Send message with location
                chatRepository.sendMessage(
                    conversationId,
                    "Location",
                    MessageType.LOCATION,
                    location = location
                )
                
                _uiState.value = _uiState.value.copy(isSendingMessage = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    errorMessage = "Failed to send location: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Sends an audio message in the current conversation
     * @param audioUri URI of the recorded audio
     */
    fun sendAudioMessage(audioUri: Uri) {
        val conversationId = _conversationId.value ?: return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true)
        _uploadProgress.value = 0f
        
        viewModelScope.launch {
            try {
                // Upload audio to storage
                val audioUrl = storageRepository.uploadAudio(
                    audioUri,
                    "conversations/$conversationId/audio",
                    onProgressChanged = { progress -> _uploadProgress.value = progress }
                )
                
                // Send message with audio URL
                chatRepository.sendMessage(
                    conversationId,
                    "Audio message",
                    MessageType.AUDIO,
                    mediaUrl = audioUrl
                )
                
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    isRecordingAudio = false
                )
                _uploadProgress.value = null
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingMessage = false,
                    isRecordingAudio = false,
                    errorMessage = "Failed to send audio: ${e.localizedMessage}"
                )
                _uploadProgress.value = null
            }
        }
    }

    /**
     * Updates the audio recording state
     * @param isRecording Whether audio is currently being recorded
     */
    fun setRecordingState(isRecording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecordingAudio = isRecording)
    }

    /**
     * Clears any error message in the UI state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Loads older messages in the conversation
     * This method implements pagination for the chat history
     */
    fun loadOlderMessages() {
        val conversationId = _conversationId.value ?: return
        if (_uiState.value.isLoadingOlderMessages || !_uiState.value.hasMoreMessagesToLoad) return
        
        _uiState.value = _uiState.value.copy(isLoadingOlderMessages = true)
        
        viewModelScope.launch {
            try {
                // Get the oldest message timestamp as a reference point
                val oldestMessageTimestamp = _uiState.value.messages.minByOrNull { 
                    it.timestamp.seconds * 1000 + it.timestamp.nanoseconds / 1000000 
                }?.timestamp
                
                // If we have messages, load messages before the oldest one
                if (oldestMessageTimestamp != null) {
                    val olderMessages = chatRepository.getOlderMessages(conversationId, oldestMessageTimestamp, 20)
                    
                    if (olderMessages.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingOlderMessages = false,
                            hasMoreMessagesToLoad = false
                        )
                    } else {
                        // Combine older messages with current messages, maintaining order
                        val combinedMessages = olderMessages + _uiState.value.messages
                        _uiState.value = _uiState.value.copy(
                            isLoadingOlderMessages = false,
                            messages = combinedMessages
                        )
                    }
                } else {
                    // If no messages yet, just mark as not loading
                    _uiState.value = _uiState.value.copy(isLoadingOlderMessages = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingOlderMessages = false,
                    errorMessage = "Failed to load more messages: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Updates the typing status and sends it to the server
     * Called when the user is typing a message
     */
    fun updateTypingStatus(isTyping: Boolean) {
        val conversationId = _conversationId.value ?: return
        
        viewModelScope.launch {
            try {
                chatRepository.setTypingStatus(conversationId, isTyping)
            } catch (e: Exception) {
                // Just log the error, don't update UI
                // Timber.e("Error updating typing status: ${e.message}")
            }
        }
    }
    
    /**
     * Starts observing the typing and online status of other users in the conversation
     */
    fun startObservingUserStatus() {
        val conversationId = _conversationId.value ?: return
        
        viewModelScope.launch {
            try {
                chatRepository.observeTypingStatus(conversationId).collect { typingUsers ->
                    // Filter out current user
                    val currentUserId = chatRepository.getCurrentUserId() ?: return@collect
                    val isOtherUserTyping = typingUsers.any { it != currentUserId }
                    
                    _uiState.value = _uiState.value.copy(isOtherUserTyping = isOtherUserTyping)
                }
            } catch (e: Exception) {
                // Just log the error
                // Timber.e("Error observing typing status: ${e.message}")
            }
        }
        
        viewModelScope.launch {
            try {
                chatRepository.observeOnlineStatus(conversationId).collect { onlineUsers ->
                    // Filter out current user
                    val currentUserId = chatRepository.getCurrentUserId() ?: return@collect
                    val isOtherUserOnline = onlineUsers.any { it != currentUserId }
                    
                    _uiState.value = _uiState.value.copy(isOtherUserOnline = isOtherUserOnline)
                }
            } catch (e: Exception) {
                // Just log the error
                // Timber.e("Error observing online status: ${e.message}")
            }
        }
    }

    /**
     * Updates the network connectivity status
     * @param isAvailable Whether the network is available
     */
    fun updateNetworkStatus(isAvailable: Boolean) {
        val previousState = _uiState.value.isNetworkAvailable
        _uiState.value = _uiState.value.copy(isNetworkAvailable = isAvailable)
        
        // If network is restored, try to resend failed messages
        if (!previousState && isAvailable) {
            resendFailedMessages()
        }
    }
    
    /**
     * Adds a message ID to the failed messages list
     * @param messageId ID of the failed message
     */
    private fun addFailedMessage(messageId: String) {
        val currentFailedMessages = _uiState.value.failedMessages.toMutableList()
        if (!currentFailedMessages.contains(messageId)) {
            currentFailedMessages.add(messageId)
            _uiState.value = _uiState.value.copy(failedMessages = currentFailedMessages)
        }
    }
    
    /**
     * Removes a message ID from the failed messages list
     * @param messageId ID of the message to remove
     */
    private fun removeFailedMessage(messageId: String) {
        val currentFailedMessages = _uiState.value.failedMessages.toMutableList()
        if (currentFailedMessages.contains(messageId)) {
            currentFailedMessages.remove(messageId)
            _uiState.value = _uiState.value.copy(failedMessages = currentFailedMessages)
        }
    }
    
    /**
     * Attempts to resend all failed messages
     */
    fun resendFailedMessages() {
        val conversationId = _conversationId.value ?: return
        val failedMessageIds = _uiState.value.failedMessages.toList()
        
        if (failedMessageIds.isEmpty()) return
        
        viewModelScope.launch {
            for (messageId in failedMessageIds) {
                try {
                    // Find the message in the current list
                    val message = _uiState.value.messages.find { it.id == messageId } ?: continue
                    
                    // Resend based on message type
                    when (message.messageType) {
                        MessageType.TEXT -> {
                            chatRepository.sendMessage(
                                conversationId, 
                                message.text, 
                                MessageType.TEXT
                            )
                        }
                        MessageType.IMAGE -> {
                            if (message.mediaUrl.isNotEmpty()) {
                                chatRepository.sendMessage(
                                    conversationId,
                                    "Image",
                                    MessageType.IMAGE,
                                    mediaUrl = message.mediaUrl
                                )
                            }
                        }
                        MessageType.LOCATION -> {
                            message.location?.let {
                                chatRepository.sendMessage(
                                    conversationId,
                                    "Location",
                                    MessageType.LOCATION,
                                    location = it
                                )
                            }
                        }
                        MessageType.AUDIO -> {
                            if (message.mediaUrl.isNotEmpty()) {
                                chatRepository.sendMessage(
                                    conversationId,
                                    "Audio message",
                                    MessageType.AUDIO,
                                    mediaUrl = message.mediaUrl
                                )
                            }
                        }
                    }
                    
                    // If successful, remove from failed messages list
                    removeFailedMessage(messageId)
                } catch (e: Exception) {
                    // If still failing, keep in the list
                    // Timber.e("Failed to resend message $messageId: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Retries sending a specific failed message
     * @param messageId ID of the message to retry
     */
    fun retryMessage(messageId: String) {
        val failedMessageIds = _uiState.value.failedMessages
        if (messageId in failedMessageIds) {
            viewModelScope.launch {
                try {
                    // Find the message in the current list
                    val message = _uiState.value.messages.find { it.id == messageId } ?: return@launch
                    val conversationId = _conversationId.value ?: return@launch
                    
                    // Set sending state
                    _uiState.value = _uiState.value.copy(isSendingMessage = true)
                    
                    // Resend based on message type
                    when (message.messageType) {
                        MessageType.TEXT -> {
                            chatRepository.sendMessage(
                                conversationId, 
                                message.text, 
                                MessageType.TEXT
                            )
                        }
                        MessageType.IMAGE -> {
                            if (message.mediaUrl.isNotEmpty()) {
                                chatRepository.sendMessage(
                                    conversationId,
                                    "Image",
                                    MessageType.IMAGE,
                                    mediaUrl = message.mediaUrl
                                )
                            }
                        }
                        MessageType.LOCATION -> {
                            message.location?.let {
                                chatRepository.sendMessage(
                                    conversationId,
                                    "Location",
                                    MessageType.LOCATION,
                                    location = it
                                )
                            }
                        }
                        MessageType.AUDIO -> {
                            if (message.mediaUrl.isNotEmpty()) {
                                chatRepository.sendMessage(
                                    conversationId,
                                    "Audio message",
                                    MessageType.AUDIO,
                                    mediaUrl = message.mediaUrl
                                )
                            }
                        }
                    }
                    
                    // If successful, remove from failed messages list
                    removeFailedMessage(messageId)
                    _uiState.value = _uiState.value.copy(isSendingMessage = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        errorMessage = "Failed to resend message: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Clean up any ongoing operations when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing uploads or operations if needed
        _uploadProgress.value = null
    }
}