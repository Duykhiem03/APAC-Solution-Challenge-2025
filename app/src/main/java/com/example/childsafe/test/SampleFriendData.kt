package com.example.childsafe.test

import com.example.childsafe.data.model.FriendRequest
import com.example.childsafe.data.model.FriendRequestStatus
import com.example.childsafe.data.model.UserProfile
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Sample data for testing the friend request features
 */
object SampleFriendData {
    val currentUser = UserProfile(
        userId = "current_user_id",
        displayName = "Current User",
        phoneNumber = "+1234567890",
        isOnline = true
    )
    
    val friends = listOf(
        UserProfile(
            userId = "friend1",
            displayName = "Alice Johnson",
            phoneNumber = "+12345678901",
            isOnline = true
        ),
        UserProfile(
            userId = "friend2",
            displayName = "Bob Smith",
            phoneNumber = "+12345678902",
            isOnline = false
        ),
        UserProfile(
            userId = "friend3",
            displayName = "Carol Williams",
            phoneNumber = "+12345678903",
            isOnline = true
        )
    )
    
    val receivedRequests = listOf(
        FriendRequest(
            requestId = "req1",
            senderId = "user1",
            recipientId = "current_user_id",
            status = FriendRequestStatus.PENDING,
            message = "Hi, I'd like to be friends!",
            createdAt = Timestamp(Date()),
            senderProfile = UserProfile(
                userId = "user1",
                displayName = "David Brown",
                phoneNumber = "+12345678904"
            )
        ),
        FriendRequest(
            requestId = "req2",
            senderId = "user2",
            recipientId = "current_user_id",
            status = FriendRequestStatus.PENDING,
            message = "",
            createdAt = Timestamp(Date()),
            senderProfile = UserProfile(
                userId = "user2",
                displayName = "Emma Davis",
                phoneNumber = "+12345678905"
            )
        )
    )
    
    val sentRequests = listOf(
        FriendRequest(
            requestId = "sent1",
            senderId = "current_user_id",
            recipientId = "user3",
            status = FriendRequestStatus.PENDING,
            message = "Let's connect!",
            createdAt = Timestamp(Date())
        ),
        FriendRequest(
            requestId = "sent2",
            senderId = "current_user_id", 
            recipientId = "user4",
            status = FriendRequestStatus.PENDING,
            message = "",
            createdAt = Timestamp(Date())
        )
    )
    
    val searchResults = listOf(
        UserProfile(
            userId = "search1",
            displayName = "Frank Wilson",
            phoneNumber = "+12345678906",
            isOnline = true
        ),
        UserProfile(
            userId = "search2",
            displayName = "Grace Lee",
            phoneNumber = "+12345678907",
            isOnline = false
        ),
        UserProfile(
            userId = "search3",
            displayName = "Henry Martinez",
            phoneNumber = "+12345678908",
            isOnline = true
        )
    )
}
