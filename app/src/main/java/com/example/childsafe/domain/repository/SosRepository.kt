package com.example.childsafe.domain.repository

import com.example.childsafe.data.model.EmergencyContact
import com.example.childsafe.data.model.NotificationRecord
import com.example.childsafe.data.model.SosContactsConfig
import com.example.childsafe.data.model.SosContextData
import com.example.childsafe.data.model.SosEvent
import com.example.childsafe.data.model.SosLocation
import com.example.childsafe.data.model.TriggerMethod

/**
 * Repository interface for SOS functionality
 * Defines methods for interacting with SOS data sources
 */
interface SosRepository {

    /**
     * Gets SOS contacts configuration for the current user
     * @return SosContactsConfig containing emergency contacts and settings
     */
    suspend fun getSosContactsConfig(): SosContactsConfig

    /**
     * Gets the active SOS event for the current user if any
     * @return SosEvent or null if no active event
     */
    suspend fun getActiveSOSEvent(): SosEvent?

    /**
     * Triggers a new SOS event
     * @param location Current location of the user
     * @param triggerMethod How the SOS was triggered
     * @param contextData Additional context for the SOS event
     * @return The created SosEvent
     */
    suspend fun triggerSosEvent(
        location: SosLocation,
        triggerMethod: TriggerMethod,
        contextData: SosContextData
    ): SosEvent

    /**
     * Resolves an active SOS event
     * @param sosEventId ID of the SOS event to resolve
     * @param isFalseAlarm Whether the SOS was a false alarm
     * @return Whether the operation was successful
     */
    suspend fun resolveSosEvent(
        sosEventId: String,
        isFalseAlarm: Boolean = false
    ): Boolean

    /**
     * Updates emergency contacts for the current user
     * @param contacts List of emergency contacts
     * @return Whether the operation was successful
     */
    suspend fun updateEmergencyContacts(
        contacts: List<EmergencyContact>
    ): Boolean

    /**
     * Records that a notification was sent for an SOS event
     * @param sosEventId ID of the SOS event
     * @param notificationRecord Details of the notification
     * @return Whether the operation was successful
     */
    suspend fun recordSosNotification(
        sosEventId: String,
        notificationRecord: NotificationRecord
    ): Boolean
}