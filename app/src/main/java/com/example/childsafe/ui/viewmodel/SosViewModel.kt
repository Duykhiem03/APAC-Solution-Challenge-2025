package com.example.childsafe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.model.EmergencyContact
import com.example.childsafe.data.model.SosContextData
import com.example.childsafe.data.model.SosEvent
import com.example.childsafe.data.model.SosLocation
import com.example.childsafe.data.model.TriggerMethod
import com.example.childsafe.domain.repository.LocationRepository
import com.example.childsafe.domain.repository.SosRepository
import com.example.childsafe.services.SosMonitoringService
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * SosViewModel is responsible for managing the SOS functionality
 * and providing data for the SOS button and emergency triggers.
 *
 * This ViewModel:
 * - Handles SOS button press and confirmation countdown
 * - Manages SOS event triggering and notifications
 * - Provides access to emergency contacts
 * - Tracks SOS events and their status
 * - Handles both manual and automatic SOS triggering
 */
@HiltViewModel
class SosViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val locationRepository: LocationRepository,
    private val sosMonitoringService: SosMonitoringService
) : ViewModel() {

    // UI State for SOS functionality
    data class SosUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val emergencyContacts: List<EmergencyContact> = emptyList(),
        val activeSOSEvent: SosEvent? = null,
        val isSosButtonPressed: Boolean = false,
        val sosCountdownSeconds: Int = 0,
        val confirmationDelaySeconds: Int = 10,
        val isSOSActive: Boolean = false,
        val isCancelling: Boolean = false
    )
    
    // MutableStateFlow to hold the current UI state
    private val _uiState = MutableStateFlow(SosUiState())
    
    // Public StateFlow for the UI to observe
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()
    
    // Countdown job for SOS confirmation delay
    private var countdownJob: Job? = null

    init {
        // Load emergency contacts and settings when ViewModel is created
        loadEmergencyContacts()
        // Check if there's an active SOS event
        checkForActiveSosEvent()
    }

    /**
     * Loads emergency contacts for the current user
     */
    fun loadEmergencyContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val contactsConfig = sosRepository.getSosContactsConfig()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    emergencyContacts = contactsConfig.contacts,
                    confirmationDelaySeconds = contactsConfig.sosSettings.confirmationDelaySeconds,
                    errorMessage = null
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load emergency contacts: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Check if there's currently an active SOS event for the user
     */
    private fun checkForActiveSosEvent() {
        viewModelScope.launch {
            try {
                val activeSosEvent = sosRepository.getActiveSOSEvent()
                if (activeSosEvent != null) {
                    _uiState.value = _uiState.value.copy(
                        activeSOSEvent = activeSosEvent,
                        isSOSActive = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to check for active SOS events: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Initiates SOS button press countdown
     */
    fun pressSosButton() {
        if (_uiState.value.isSosButtonPressed || _uiState.value.isSOSActive) return
        
        _uiState.value = _uiState.value.copy(
            isSosButtonPressed = true,
            sosCountdownSeconds = _uiState.value.confirmationDelaySeconds
        )
        
        // Start countdown
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.sosCountdownSeconds > 0) {
                delay(1000) // 1 second delay
                _uiState.value = _uiState.value.copy(
                    sosCountdownSeconds = _uiState.value.sosCountdownSeconds - 1
                )
            }
            
            // Trigger SOS when countdown completes
            if (_uiState.value.isSosButtonPressed) {
                triggerSOS()
            }
        }
    }

    /**
     * Cancels the ongoing SOS button press countdown
     */
    fun cancelSosButtonPress() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isSosButtonPressed = false,
            sosCountdownSeconds = 0
        )
    }

    /**
     * Triggers an SOS event manually
     */
    private fun triggerSOS() {
        if (_uiState.value.isSOSActive) return
        
        viewModelScope.launch {
            try {
                // Get current location
                val currentLocation = locationRepository.getCurrentLocation()
                
                // Create location object for SOS event
                val sosLocation = SosLocation(
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude,
                    accuracy = currentLocation.accuracy,
                    address = "" // Will be reverse geocoded by the backend
                )
                
                // Additional context data
                val isMoving = locationRepository.isDeviceMoving()
                val speed = locationRepository.getCurrentSpeed()

                // Create SosContextData object
                val sosContextData = SosContextData(moving = isMoving, speed = speed)
                
                // Trigger the SOS event
                val sosEvent = sosRepository.triggerSosEvent(
                    location = sosLocation,
                    triggerMethod = TriggerMethod.MANUAL,
                    contextData = sosContextData
                )
                
                _uiState.value = _uiState.value.copy(
                    isSOSActive = true,
                    isSosButtonPressed = false,
                    activeSOSEvent = sosEvent
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSosButtonPressed = false,
                    errorMessage = "Failed to trigger SOS: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Starts monitoring and sending continuous location updates for an active SOS event
     * @param location Initial location of the SOS event
     * @param conversationIds List of conversation IDs that received the initial SOS message
     */
    fun startSosMonitoring(location: LatLng, conversationIds: List<String>) {
        val activeEvent = _uiState.value.activeSOSEvent ?: return
        
        // Update the SOS event with conversation IDs for later location updates
        viewModelScope.launch {
            try {
                // Update the SOS event with conversation IDs
                val updatedEvent = activeEvent.copy(
                    conversationIds = conversationIds
                )
                
                // Update in repository if possible
                sosRepository.updateSosEvent(updatedEvent)
                
                // Start monitoring service for continuous location updates
                sosMonitoringService.startMonitoring(updatedEvent)
            } catch (e: Exception) {
                // Even if updating in repository fails, we can still start monitoring
                // with the conversation IDs we have
                sosMonitoringService.startMonitoring(
                    activeEvent.copy(conversationIds = conversationIds)
                )
            }
        }
    }

    /**
     * Resolves the active SOS event
     * @param isFalseAlarm Whether the SOS was a false alarm
     */
    fun resolveSosEvent(isFalseAlarm: Boolean = false) {
        val activeEvent = _uiState.value.activeSOSEvent ?: return
        
        _uiState.value = _uiState.value.copy(isCancelling = true)
        
        viewModelScope.launch {
            try {
                // Stop the monitoring service first
                sosMonitoringService.stopMonitoring()
                
                // Then resolve the SOS event in the repository
                sosRepository.resolveSosEvent(
                    sosEventId = activeEvent.id,
                    isFalseAlarm = isFalseAlarm
                )
                
                _uiState.value = _uiState.value.copy(
                    isSOSActive = false,
                    isCancelling = false,
                    activeSOSEvent = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    errorMessage = "Failed to resolve SOS event: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Checks if there are any emergency contacts configured
     * @return True if at least one emergency contact is configured
     */
    fun hasEmergencyContacts(): Boolean {
        return _uiState.value.emergencyContacts.isNotEmpty()
    }
    
    /**
     * Updates the user's emergency contacts
     * @param contacts New list of emergency contacts
     */
    fun updateEmergencyContacts(contacts: List<EmergencyContact>) {
        viewModelScope.launch {
            try {
                val success = sosRepository.updateEmergencyContacts(contacts)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        emergencyContacts = contacts,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to update emergency contacts"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error updating emergency contacts: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Immediately triggers an SOS event with the provided location
     * This skips the countdown and directly triggers the SOS alert
     * @param location Current location to use for the SOS event
     */
    fun triggerSOSImmediately(location: LatLng) {
        if (_uiState.value.isSOSActive) return
        
        viewModelScope.launch {
            try {
                // Create location object for SOS event from provided location
                val sosLocation = SosLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = 0f, // We don't have accuracy from LatLng
                    address = "" // Will be reverse geocoded by the backend
                )
                
                // Get additional context data
                val isMoving = locationRepository.isDeviceMoving()
                val speed = locationRepository.getCurrentSpeed()

                // Create SosContextData object
                val sosContextData = SosContextData(moving = isMoving, speed = speed)
                
                // Trigger the SOS event immediately
                val sosEvent = sosRepository.triggerSosEvent(
                    location = sosLocation,
                    triggerMethod = TriggerMethod.MANUAL_IMMEDIATE, // Use a specific trigger method for immediate SOS
                    contextData = sosContextData
                )
                
                _uiState.value = _uiState.value.copy(
                    isSOSActive = true,
                    isSosButtonPressed = false,
                    activeSOSEvent = sosEvent
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to trigger immediate SOS: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Clears any error message in the UI state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Called when ViewModel is cleared to cancel any ongoing operations
     */
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}