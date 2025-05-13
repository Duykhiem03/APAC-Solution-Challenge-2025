package com.example.childsafe.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a user's FCM token in Firestore
 * Used to store device-specific tokens for sending notifications
 */
data class UserFcmToken(
    @DocumentId
    val id: String = "",
    
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("token")
    val token: String = "",
    
    @PropertyName("deviceInfo")
    val deviceInfo: String = "",
    
    @PropertyName("lastUpdated")
    val lastUpdated: Any? = null, // This can be a Date or FieldValue.serverTimestamp()
    
    @PropertyName("platform")
    val platform: String = "android",
    
    @PropertyName("isActive")
    val isActive: Boolean = true
)