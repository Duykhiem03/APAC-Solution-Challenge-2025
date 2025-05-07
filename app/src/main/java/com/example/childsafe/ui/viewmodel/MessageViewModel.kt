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
        val hasLocationPermission: Boolean = false
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
     * Clean up any ongoing operations when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing uploads or operations if needed
        _uploadProgress.value = null
    }
}