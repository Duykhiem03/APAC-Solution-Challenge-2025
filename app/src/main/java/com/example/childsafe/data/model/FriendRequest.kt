package com.example.childsafe.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Represents a friend request/invitation in the system
 */
data class FriendRequest(
    @DocumentId
    val requestId: String = "",
    
    @PropertyName("senderId")
    val senderId: String = "",
    
    @PropertyName("recipientId")
    val recipientId: String = "",
    
    @PropertyName("status")
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    
    @PropertyName("message")
    val message: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null,
    
    // This is used for easier UI display - populated from the UserProfile
    var senderProfile: UserProfile? = null
)

/**
 * Status of a friend request
 */
enum class FriendRequestStatus {
    PENDING,   // Request has been sent but not accepted or rejected
    ACCEPTED,  // Request has been accepted
    REJECTED,  // Request has been rejected
    BLOCKED    // Request was rejected and sender is blocked
}
