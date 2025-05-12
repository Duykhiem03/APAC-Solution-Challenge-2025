package com.example.childsafe.utils

import com.example.childsafe.data.model.MessageStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Simple EventBus implementation using Kotlin Flows
 * Used for communication between components that aren't directly connected
 */
object EventBusManager {
    // Event flow for message status updates
    private val _messageStatusFlow = MutableSharedFlow<StatusUpdateEvent>(
        extraBufferCapacity = 8
    )
    val messageStatusFlow = _messageStatusFlow.asSharedFlow()
    
    // Event flow for new messages (real-time updates)
    private val _newMessageFlow = MutableSharedFlow<NewMessageEvent>(
        extraBufferCapacity = 16
    )
    val newMessageFlow = _newMessageFlow.asSharedFlow()
    
    // Event flow for user presence updates
    private val _userPresenceFlow = MutableSharedFlow<UserPresenceEvent>(
        extraBufferCapacity = 16
    )
    val userPresenceFlow = _userPresenceFlow.asSharedFlow()
    
    // Post an event to the status update flow
    suspend fun post(event: StatusUpdateEvent) {
        _messageStatusFlow.emit(event)
    }
    
    // Post a new message event to the flow
    suspend fun post(event: NewMessageEvent) {
        _newMessageFlow.emit(event)
        Timber.d("Posted new message event: ${event.message.id}")
    }
    
    // Post a user presence event to the flow
    suspend fun post(event: UserPresenceEvent) {
        _userPresenceFlow.emit(event)
        Timber.d("Posted user presence event: ${event.userId} is ${if (event.isOnline) "online" else "offline"}")
    }
    
    // Non-suspending version for use from any thread
    fun post(event: StatusUpdateEvent, onError: ((Throwable) -> Unit)? = null) {
        try {
            // Use emit() if the SharedFlow has space available
            if (_messageStatusFlow.tryEmit(event)) {
                return
            }
            
            // Otherwise launch a coroutine to emit the event
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    _messageStatusFlow.emit(event)
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }
    
    // Non-suspending version for presence updates from any thread
    fun postPresence(event: UserPresenceEvent, onError: ((Throwable) -> Unit)? = null) {
        try {
            // Use emit() if the SharedFlow has space available
            if (_userPresenceFlow.tryEmit(event)) {
                return
            }
            
            // Otherwise launch a coroutine to emit the event
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    _userPresenceFlow.emit(event)
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }
    
    // Post an event to the conversation read flow (non-suspending)
    fun post(event: ConversationReadEvent, onError: ((Throwable) -> Unit)? = null) {
        // Similar implementation to handle conversation read events
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // For now, we're just logging these events
                Timber.d("Conversation read event: ${event.conversationId} by ${event.readerName}")
                // In a full implementation, we'd emit to a dedicated flow for conversation read events
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }
}

/**
 * Event for message status updates
 */
data class StatusUpdateEvent(
    val messageId: String,
    val newStatus: String
)

/**
 * Event for conversation read status updates
 */
data class ConversationReadEvent(
    val conversationId: String,
    val readerId: String,
    val readerName: String
)


/**
 * Event for user presence updates (online/offline status changes)
 */
data class UserPresenceEvent(
    val userId: String,
    val isOnline: Boolean,
    val conversationId: String? = null, // Optional: if specific to a conversation
    val lastSeenTimestamp: com.google.firebase.Timestamp? = null,
    val isTyping: Boolean = false
)

