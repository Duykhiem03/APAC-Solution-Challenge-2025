package com.example.childsafe.services

import android.content.Context
import android.location.Location
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.model.SosEvent
import com.example.childsafe.data.model.SosLocation
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.LocationRepository
import com.example.childsafe.domain.repository.SosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing active SOS events and location updates
 */
@Singleton
class SosMonitoringService @Inject constructor(
    private val context: Context,
    private val sosRepository: SosRepository,
    private val chatRepository: ChatRepository,
    private val locationRepository: LocationRepository,
    private val sosNotificationService: SosNotificationService
) {
    private var activeSOSEvent: SosEvent? = null
    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    // Constants
    companion object {
        const val UPDATE_INTERVAL_MS = 10000L // 10 seconds
    }
    
    /**
     * Starts monitoring an active SOS event
     * @param sosEvent The active SOS event to monitor
     */
    fun startMonitoring(sosEvent: SosEvent) {
        // Store the active SOS event
        activeSOSEvent = sosEvent
        
        // Show persistent notification
        sosNotificationService.showActiveSosNotification(sosEvent)
        
        // Cancel any existing job
        stopMonitoring(false)
        
        // Start location updates
        updateJob = serviceScope.launch {
            Timber.d("SosMonitoringService: Starting location updates every ${UPDATE_INTERVAL_MS/1000} seconds")
            
            while (isActive) {
                try {
                    // Check if event is still active
                    val activeEvent = sosRepository.getActiveSOSEvent()
                    if (activeEvent == null) {
                        Timber.d("SosMonitoringService: SOS event no longer active, stopping updates")
                        stopMonitoring(true)
                        break
                    }
                    
                    // Get current location
                    val currentLocation = locationRepository.getCurrentLocation()
                    
                    // Update SOS event location in the database
                    val updatedLocation = SosLocation(
                        latitude = currentLocation.latitude,
                        longitude = currentLocation.longitude,
                        accuracy = currentLocation.accuracy
                    )
                    
                    sosRepository.updateSosLocation(sosEvent.id, updatedLocation)
                    
                    // Send location update message to emergency contacts
                    if (sosEvent.conversationIds != null && sosEvent.conversationIds.isNotEmpty()) {
                        sosEvent.conversationIds.forEach { conversationId ->
                            sendLocationUpdate(conversationId, currentLocation)
                        }
                    } else {
                        // If no specific conversations are set, try to send to emergency contacts
                        try {
                            // Get emergency contacts
                            val contactsConfig = sosRepository.getSosContactsConfig()
                            val emergencyContactIds = contactsConfig.contacts.map { it.contactId }
                            
                            // Get all conversations and filter for emergency contacts
                            val conversations = chatRepository.getAllConversations()
                            val emergencyConversations = conversations.filter { conversation ->
                                // For direct conversations, check if other user is an emergency contact
                                if (!conversation.isGroup && conversation.participants.size == 2) {
                                    val currentUserId = chatRepository.getCurrentUserIdSync() ?: return@filter false
                                    val otherParticipantId = conversation.participants.find { it != currentUserId } ?: return@filter false
                                    return@filter emergencyContactIds.contains(otherParticipantId)
                                }
                                false
                            }
                            
                            // Send updates to emergency contacts
                            emergencyConversations.forEach { conversation ->
                                sendLocationUpdate(conversation.id, currentLocation)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to determine emergency contacts for location updates")
                        }
                    }
                    
                    // Wait for the next update interval
                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Timber.e(e, "Error during SOS location update")
                    // If there's an error, still try to continue after a delay
                    delay(UPDATE_INTERVAL_MS)
                }
            }
        }
    }
    
    /**
     * Sends a location update message to a specific conversation
     */
    private suspend fun sendLocationUpdate(conversationId: String, location: Location) {
        try {
            val messageLocation = MessageLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                locationName = "SOS Location Update"
            )
            
            chatRepository.sendMessage(
                conversationId = conversationId,
                text = "SOS location update",
                messageType = MessageType.LOCATION,
                mediaUrl = null,
                location = messageLocation
            )
            
            Timber.d("SOS location update sent to conversation: $conversationId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send SOS location update to conversation: $conversationId")
        }
    }
    
    /**
     * Stops monitoring the active SOS event
     * @param dismissNotification Whether to dismiss the notification
     */
    fun stopMonitoring(dismissNotification: Boolean = true) {
        // Cancel the update job
        updateJob?.cancel()
        updateJob = null
        
        // Clear active event
        activeSOSEvent = null
        
        // Dismiss notification if requested
        if (dismissNotification) {
            sosNotificationService.dismissActiveSosNotification()
        }
        
        Timber.d("SosMonitoringService: Stopped monitoring SOS event")
    }
    
    /**
     * Checks if there's an active SOS event being monitored
     */
    fun isMonitoringActive(): Boolean {
        return updateJob?.isActive == true && activeSOSEvent != null
    }
}
