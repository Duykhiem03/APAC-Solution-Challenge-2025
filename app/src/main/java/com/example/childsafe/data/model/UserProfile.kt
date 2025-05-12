package com.example.childsafe.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Represents a user profile in the system
 */
data class UserProfile(
    @DocumentId
    val userId: String = "",
    
    @PropertyName("displayName")
    val displayName: String = "",
    
    @PropertyName("phoneNumber")
    val phoneNumber: String = "",
    
    @PropertyName("photoUrl")
    val photoUrl: String = "",
    
    @PropertyName("isOnline")
    val isOnline: Boolean = false,
    
    @PropertyName("lastActive")
    val lastActive: Timestamp? = null,
    
    @PropertyName("profileVisibility")
    val profileVisibility: ProfileVisibility = ProfileVisibility.CONTACTS_ONLY,
    
    @PropertyName("allowSearchByPhone")
    val allowSearchByPhone: Boolean = true,
    
    @PropertyName("allowSearchByName")
    val allowSearchByName: Boolean = true,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null
)

/**
 * Enum for defining who can see a user's profile
 */
enum class ProfileVisibility {
    PUBLIC,        // Anyone can find the user
    CONTACTS_ONLY, // Only contacts can see full profile
    PRIVATE        // Profile is completely private
}
