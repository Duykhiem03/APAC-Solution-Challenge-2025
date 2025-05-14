package com.example.childsafe.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data models for SOS Feature
 * These models correspond to the Firestore schema defined in sosDataSchema.js
 */

/**
 * Represents user's emergency contacts and SOS settings
 */
data class SosContactsConfig(
    @DocumentId val userId: String = "",
    val contacts: List<EmergencyContact> = emptyList(),
    val sosSettings: SosSettings = SosSettings()
)

/**
 * Represents an individual emergency contact
 */
data class EmergencyContact(
    val contactId: String = "",
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
    val notificationPreferences: NotificationPreferences = NotificationPreferences()
)

/**
 * Contact's notification preferences
 */
data class NotificationPreferences(
    val receivePushNotifications: Boolean = true,
    val receiveSMS: Boolean = true,
    val receiveEmails: Boolean = false,
    val notificationPriority: NotificationPriority = NotificationPriority.HIGH
)

/**
 * Priority levels for notifications
 */
enum class NotificationPriority {
    @PropertyName("high")
    HIGH,
    
    @PropertyName("medium")
    MEDIUM,
    
    @PropertyName("low")
    LOW
}

/**
 * User's SOS feature settings
 */
data class SosSettings(
    val automaticSosDetection: Boolean = true,
    val confirmationDelaySeconds: Int = 10,
    val includeLocation: Boolean = true,
    val includeCameraSnapshot: Boolean = false,
    val includeAudioRecording: Boolean = false
)

/**
 * Represents an SOS emergency event
 */
data class SosEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val location: SosLocation = SosLocation(),
    val triggerMethod: TriggerMethod = TriggerMethod.MANUAL,
    val status: SosStatus = SosStatus.ACTIVE,
    val resolvedAt: Timestamp? = null,
    val resolvedBy: String? = null,
    val notificationsSent: List<NotificationRecord> = emptyList(),
    val contextData: SosContextData = SosContextData(),
    val conversationIds: List<String>? = null // Conversation IDs that have received SOS messages
)

/**
 * Location information for SOS events
 */
data class SosLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String = ""
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

/**
 * Method that triggered the SOS
 */
enum class TriggerMethod {
    @PropertyName("manual")
    MANUAL,
    
    @PropertyName("geofence")
    GEOFENCE,
    
    @PropertyName("activity")
    ACTIVITY,
    
    @PropertyName("aiDetection")
    AI_DETECTION
}

/**
 * Status of an SOS event
 */
enum class SosStatus {
    @PropertyName("active")
    ACTIVE,
    
    @PropertyName("responded")
    RESPONDED,
    
    @PropertyName("resolved")
    RESOLVED,
    
    @PropertyName("false_alarm")
    FALSE_ALARM
}

/**
 * Record of notifications sent during an SOS event
 */
data class NotificationRecord(
    val contactId: String = "",
    val sentAt: Timestamp = Timestamp.now(),
    val methods: List<String> = emptyList(), // "push", "sms", "email"
    val acknowledgedAt: Timestamp? = null
)

/**
 * Context data about the device and user during an SOS event
 */
data class SosContextData(
    val batteryLevel: Int = 0,
    val moving: Boolean = false,
    val speed: Float = 0f,
    val recentLocations: List<RecentLocation> = emptyList(),
    val aiDetectedContext: String = ""
)

/**
 * Location history entry
 */
data class RecentLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now()
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

/**
 * Geofence zone for SOS triggering
 */
data class GeofenceZone(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: GeofenceType = GeofenceType.SAFE,
    val shape: GeofenceShape = GeofenceShape.CIRCLE,
    val center: GeoPoint? = null,
    val radius: Int = 0, // Meters
    val points: List<GeoPoint> = emptyList(),
    val schedule: GeofenceSchedule = GeofenceSchedule(),
    val actions: GeofenceActions = GeofenceActions()
)

/**
 * Type of geofence
 */
enum class GeofenceType {
    @PropertyName("safe")
    SAFE,
    
    @PropertyName("unsafe")
    UNSAFE
}

/**
 * Shape of geofence
 */
enum class GeofenceShape {
    @PropertyName("circle")
    CIRCLE,
    
    @PropertyName("polygon")
    POLYGON
}

/**
 * Geographic point for geofence
 */
data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

/**
 * Schedule when geofence is active
 */
data class GeofenceSchedule(
    val enabled: Boolean = true,
    val days: List<Int> = listOf(1, 2, 3, 4, 5), // 0 = Sunday, 1 = Monday, etc.
    val startTime: String = "08:00",
    val endTime: String = "15:00"
)

/**
 * Actions to take when geofence is triggered
 */
data class GeofenceActions(
    val notifyContacts: Boolean = true,
    val triggerSos: Boolean = false,
    val sendWarning: Boolean = true
)

// IMPLEMENTATION NOTES:
// 1. These data models should be used with Firebase Firestore
// 2. The GeofenceZone class requires integration with Google's Geofencing API
//    for active monitoring on the device
// 3. TriggerMethod.AI_DETECTION would need integration with your AI microservice
// 4. Consider implementing helper functions for common operations:
//    - Calculate if a location is inside a geofence
//    - Check if a geofence is currently active based on schedule
//    - Convert Firestore documents to these data classes