package com.example.childsafe.data.repository

import com.example.childsafe.data.model.EmergencyContact
import com.example.childsafe.data.model.NotificationRecord
import com.example.childsafe.data.model.SosContactsConfig
import com.example.childsafe.data.model.SosContextData
import com.example.childsafe.data.model.SosEvent
import com.example.childsafe.data.model.SosLocation
import com.example.childsafe.data.model.SosSettings
import com.example.childsafe.data.model.SosStatus
import com.example.childsafe.data.model.TriggerMethod
import com.example.childsafe.domain.repository.SosRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SosRepository that interacts with Firebase Firestore
 * Handles SOS events and emergency contact management
 */
@Singleton
class SosRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SosRepository {

    // Collection references
    private val sosEventsCollection = firestore.collection("sosEvents")
    private val sosContactsCollection = firestore.collection("sosContacts")

    // Current user ID
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    /**
     * Gets SOS contacts configuration for the current user
     */
    override suspend fun getSosContactsConfig(): SosContactsConfig {
        val contactsDoc = sosContactsCollection.document(currentUserId).get().await()
        
        return if (contactsDoc.exists()) {
            contactsDoc.toObject(SosContactsConfig::class.java)
                ?: SosContactsConfig(userId = currentUserId)
        } else {
            // Create default config if none exists
            val defaultConfig = SosContactsConfig(
                userId = currentUserId,
                contacts = emptyList(),
                sosSettings = SosSettings()
            )
            
            // Save default config to Firestore
            sosContactsCollection.document(currentUserId).set(defaultConfig).await()
            
            defaultConfig
        }
    }

    /**
     * Gets the active SOS event for the current user if any
     */
    override suspend fun getActiveSOSEvent(): SosEvent? {
        val activeEvents = sosEventsCollection
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("status", SosStatus.ACTIVE.name.lowercase())
            .get()
            .await()
            
        return activeEvents.documents.firstOrNull()?.toObject(SosEvent::class.java)
    }

    /**
     * Triggers a new SOS event
     */
    override suspend fun triggerSosEvent(
        location: SosLocation,
        triggerMethod: TriggerMethod,
        contextData: SosContextData
    ): SosEvent {
        // Check if there's already an active SOS event
        val existingEvent = getActiveSOSEvent()
        if (existingEvent != null) {
            return existingEvent
        }
        
        // Create new SOS event
        val sosEvent = SosEvent(
            userId = currentUserId,
            timestamp = Timestamp.now(),
            location = location,
            triggerMethod = triggerMethod,
            status = SosStatus.ACTIVE,
            contextData = contextData
        )
        
        // Add to Firestore
        val eventDoc = sosEventsCollection.add(sosEvent).await()
        
        // Return with document ID
        return sosEvent.copy(id = eventDoc.id)
    }

    /**
     * Resolves an active SOS event
     */
    override suspend fun resolveSosEvent(
        sosEventId: String,
        isFalseAlarm: Boolean
    ): Boolean {
        return try {
            sosEventsCollection.document(sosEventId).update(
                mapOf(
                    "status" to if (isFalseAlarm) SosStatus.FALSE_ALARM.name.lowercase() else SosStatus.RESOLVED.name.lowercase(),
                    "resolvedAt" to Timestamp.now(),
                    "resolvedBy" to currentUserId
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates emergency contacts for the current user
     */
    override suspend fun updateEmergencyContacts(
        contacts: List<EmergencyContact>
    ): Boolean {
        return try {
            val contactsDoc = sosContactsCollection.document(currentUserId).get().await()
            
            if (contactsDoc.exists()) {
                // Update existing contacts
                sosContactsCollection.document(currentUserId).update(
                    "contacts", contacts
                ).await()
            } else {
                // Create new contacts config
                sosContactsCollection.document(currentUserId).set(
                    SosContactsConfig(
                        userId = currentUserId,
                        contacts = contacts,
                        sosSettings = SosSettings()
                    )
                ).await()
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Records that a notification was sent for an SOS event
     */
    override suspend fun recordSosNotification(
        sosEventId: String,
        notificationRecord: NotificationRecord
    ): Boolean {
        return try {
            sosEventsCollection.document(sosEventId).update(
                "notificationsSent", FieldValue.arrayUnion(notificationRecord)
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}