package com.example.childsafe.services

import android.content.Context
import com.example.childsafe.data.local.OfflineMessage
import com.example.childsafe.data.local.OfflineMessageDao
import com.example.childsafe.data.local.OfflineMessageStatus
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Service responsible for synchronizing messages between local storage and Firebase
 * when offline and reconnecting
 */
@Singleton
class MessageSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineMessageDao: OfflineMessageDao,
    private val chatRepositoryLazy: Lazy<ChatRepository>, // Using Lazy to break dependency cycle
    private val connectionManager: ConnectionManager,
    private val auth: FirebaseAuth
) {
    // Access to repository using lazy property
    private val chatRepository: ChatRepository by lazy { chatRepositoryLazy.get() }
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Controls whether the sync service is running
    private val isRunning = AtomicBoolean(false)
    
    // Retry configuration
    private val baseRetryDelayMs = 5000L // 5 seconds
    private val maxRetryDelayMs = 60000L // 1 minute
    private val maxRetries = 5

    // Network restoration callback
    private val networkRestoreCallback: () -> Unit = {
        syncScope.launch {
            Timber.d("Network restored, initiating message synchronization")
            syncMessages()
            requestImmediateRetry()
        }
    }
    
    init {
        // Register for network restore events
        connectionManager.registerNetworkRestoreListener(networkRestoreCallback)
    }
    
    /**
     * Start the message synchronization service
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            Timber.d("MessageSyncService already running")
            return
        }
        
        Timber.d("Starting MessageSyncService")
        
        // Monitor connection changes and sync when connection is restored
        syncScope.launch {
            connectionManager.connectionState.collectLatest { isConnected ->
                if (isConnected) {
                    Timber.d("Connection restored, starting message sync")
                    syncMessages()
                }
            }
        }
    }
    
    /**
     * Stop the message synchronization service
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        
        Timber.d("Stopping MessageSyncService")
        
        // Unregister from network restore events
        connectionManager.unregisterNetworkRestoreListener(networkRestoreCallback)
        
        syncScope.cancel()
    }
    
    /**
     * Queue a message to be sent when the connection is restored
     */
    suspend fun queueMessage(
        conversationId: String,
        text: String,
        messageType: com.example.childsafe.data.model.MessageType,
        mediaUrl: String? = null,
        location: MessageLocation? = null
    ): String {
        Timber.d("Queueing offline message for conversation: $conversationId")
        
        val messageId = UUID.randomUUID().toString()
        
        val locationMap = location?.let {
            mapOf(
                "latitude" to it.latitude,
                "longitude" to it.longitude,
                "locationName" to it.locationName
            )
        }
        
        val offlineMessage = OfflineMessage(
            id = messageId,
            conversationId = conversationId,
            text = text,
            messageType = messageType,
            mediaUrl = mediaUrl,
            location = locationMap,
            timestamp = System.currentTimeMillis(),
            status = OfflineMessageStatus.PENDING,
            retryCount = 0,
            maxRetries = maxRetries
        )
        
        offlineMessageDao.insertMessage(offlineMessage)
        
        // If connection is available, try to send immediately
        if (connectionManager.isNetworkAvailable()) {
            syncScope.launch {
                processPendingMessages()
            }
        }
        
        return messageId
    }
    
    /**
     * Sync pending messages with the server
     */
    private suspend fun syncMessages() {
        if (!connectionManager.isNetworkAvailable()) {
            Timber.d("Cannot sync messages: No network connection")
            return
        }
        
        try {
            Timber.d("Starting message synchronization")
            
            // Process pending messages
            processPendingMessages()
            
            // Process failed messages that can be retried
            processFailedMessages()
            
            Timber.d("Message synchronization completed")
        } catch (e: Exception) {
            Timber.e(e, "Error during message synchronization")
        }
    }
    
    /**
     * Process all pending messages
     */
    private suspend fun processPendingMessages() {
        withContext(Dispatchers.IO) {
            offlineMessageDao.getMessagesByStatus(OfflineMessageStatus.PENDING).collectLatest { pendingMessages ->
                if (pendingMessages.isNotEmpty()) {
                    Timber.d("Processing ${pendingMessages.size} pending messages")
                    
                    pendingMessages.forEach { message ->
                        processMessage(message)
                    }
                }
            }
        }
    }
    
    /**
     * Process all failed messages that can be retried
     */
    private suspend fun processFailedMessages() {
        withContext(Dispatchers.IO) {
            offlineMessageDao.getMessagesByStatus(OfflineMessageStatus.FAILED).collectLatest { failedMessages ->
                if (failedMessages.isNotEmpty()) {
                    Timber.d("Processing ${failedMessages.size} failed messages")
                    
                    failedMessages.forEach { message ->
                        // Only retry if not exceeding max retries
                        if (message.retryCount < message.maxRetries) {
                            // Calculate exponential backoff delay
                            val delayMs = calculateRetryDelay(message.retryCount)
                            
                            // Check if enough time has passed since the last retry
                            val timeElapsed = System.currentTimeMillis() - message.lastRetryTimestamp
                            if (timeElapsed >= delayMs) {
                                processMessage(message)
                            }
                        } else {
                            Timber.d("Message ${message.id} exceeded maximum retry attempts")
                            // Update the message status to indicate no more retries
                            offlineMessageDao.updateMessageStatus(
                                message.id,
                                OfflineMessageStatus.CANCELED,
                                message.retryCount,
                                System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Process a single message
     */
    private suspend fun processMessage(message: OfflineMessage) {
        if (!connectionManager.isNetworkAvailable()) {
            Timber.d("Cannot process message: No network connection")
            return
        }
        
        try {
            // Mark message as being sent
            offlineMessageDao.updateMessageStatus(
                message.id,
                OfflineMessageStatus.SENDING,
                message.retryCount,
                System.currentTimeMillis()
            )
            
            // Extract location data if present
            val location = message.location?.let {
                try {
                    MessageLocation(
                        latitude = it["latitude"] as? Double ?: 0.0,
                        longitude = it["longitude"] as? Double ?: 0.0,
                        locationName = it["locationName"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing location data for message ${message.id}")
                    null
                }
            }
            
            // Send the message via the repository
            val result = chatRepository.sendMessage(
                conversationId = message.conversationId,
                text = message.text,
                messageType = message.messageType,
                mediaUrl = message.mediaUrl,
                location = location
            )
            
            if (result.isNotEmpty()) {
                // Message sent successfully
                Timber.d("Successfully sent offline message ${message.id}, server ID: $result")
                offlineMessageDao.updateMessageStatus(
                    message.id, 
                    OfflineMessageStatus.SENT,
                    message.retryCount,
                    System.currentTimeMillis()
                )
                
                // Delete the local message after successful sync
                delay(500) // Slight delay to ensure UI updates
                offlineMessageDao.deleteMessageById(message.id)
            } else {
                throw Exception("Failed to send message: Empty result")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending offline message ${message.id}")
            
            // Update message status to failed and increment retry count
            offlineMessageDao.updateMessageStatus(
                message.id,
                OfflineMessageStatus.FAILED,
                message.retryCount + 1,
                System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Calculate delay for retrying based on retry count (exponential backoff)
     */
    private fun calculateRetryDelay(retryCount: Int): Long {
        // Exponential backoff: baseDelay * 2^retryCount with a cap
        val delay = baseRetryDelayMs * (1 shl min(retryCount, 5)) // Using bit shift for 2^n
        return delay.coerceAtMost(maxRetryDelayMs)
    }
    
    /**
     * Force retry sending all failed messages
     */
    suspend fun retryFailedMessages() {
        if (!connectionManager.isNetworkAvailable()) {
            Timber.d("Cannot retry messages: No network connection")
            return
        }
        
        try {
            // Get all failed messages
            offlineMessageDao.getMessagesByStatus(OfflineMessageStatus.FAILED).collectLatest { failedMessages ->
                if (failedMessages.isNotEmpty()) {
                    Timber.d("Retrying ${failedMessages.size} failed messages")
                    
                    failedMessages.forEach { message ->
                        // Reset retry count to give it another chance
                        offlineMessageDao.updateMessageStatus(
                            message.id,
                            OfflineMessageStatus.PENDING,
                            0, // Reset retry count
                            System.currentTimeMillis()
                        )
                    }
                    
                    // Process pending messages
                    processPendingMessages()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error retrying failed messages")
        }
    }
    
    /**
     * Schedule periodic background retries
     * Call this when the app starts
     */
    fun schedulePeriodicRetries() {
        try {
            MessageRetryWorker.schedule(context)
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule periodic message retries")
        }
    }
    
    /**
     * Request an immediate background retry
     * Call this when network connection is restored
     */
    fun requestImmediateRetry() {
        try {
            if (connectionManager.isNetworkAvailable()) {
                MessageRetryWorker.runNow(context)
                Timber.d("Requested immediate retry of failed messages")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to request immediate message retry")
        }
    }
    
    /**
     * Retry a specific failed message by ID
     * @param messageId ID of the message to retry
     * @return true if the message was found and retried, false otherwise
     */
    suspend fun retryMessage(messageId: String): Boolean {
        if (!connectionManager.isNetworkAvailable()) {
            Timber.d("Cannot retry message $messageId: No network connection")
            return false
        }
        
        try {
            // Find the message by ID
            var found = false
            
            offlineMessageDao.getMessagesByStatus(OfflineMessageStatus.FAILED).collectLatest { failedMessages ->
                val message = failedMessages.find { it.id == messageId }
                
                if (message != null) {
                    Timber.d("Found failed message $messageId, resetting for retry")
                    
                    // Reset retry count and set status to pending
                    offlineMessageDao.updateMessageStatus(
                        messageId,
                        OfflineMessageStatus.PENDING,
                        0, // Reset retry count
                        System.currentTimeMillis()
                    )
                    
                    // Process the message
                    processMessage(message)
                    found = true
                }
            }
            
            if (!found) {
                Timber.d("Failed message $messageId not found in offline storage")
            }
            
            return found
        } catch (e: Exception) {
            Timber.e(e, "Error retrying message $messageId: ${e.message}")
            return false
        }
    }
}
