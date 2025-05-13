package com.example.childsafe.domain.repository

import com.example.childsafe.data.model.FriendRequest
import com.example.childsafe.data.model.FriendRequestStatus
import com.example.childsafe.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for friend-related operations
 */
interface FriendRepository {

    /**
     * Send a friend request to another user
     * @param recipientId ID of the user to send the request to
     * @param message Optional message to include with the request
     * @return The created request ID if successful, null otherwise
     */
    suspend fun sendFriendRequest(recipientId: String, message: String = ""): String?
    
    /**
     * Get all pending friend requests sent by the current user
     * @return List of friend requests
     */
    suspend fun getSentFriendRequests(): List<FriendRequest>
    
    /**
     * Get all pending friend requests received by the current user
     * @return List of friend requests
     */
    suspend fun getReceivedFriendRequests(): List<FriendRequest>
    
    /**
     * Observe received friend requests for real-time updates
     * @return Flow of friend requests lists
     */
    suspend fun observeReceivedFriendRequests(): Flow<List<FriendRequest>>
    
    /**
     * Accept a friend request
     * @param requestId ID of the request to accept
     * @return Success or failure
     */
    suspend fun acceptFriendRequest(requestId: String): Boolean
    
    /**
     * Reject a friend request
     * @param requestId ID of the request to reject
     * @param block Whether to block the sender
     * @return Success or failure
     */
    suspend fun rejectFriendRequest(requestId: String, block: Boolean = false): Boolean
    
    /**
     * Cancel a sent friend request
     * @param requestId ID of the request to cancel
     * @return Success or failure
     */
    suspend fun cancelFriendRequest(requestId: String): Boolean
    
    /**
     * Get all friends of the current user
     * @return List of friend profiles
     */
    suspend fun getFriends(): List<UserProfile>
    
    /**
     * Observe friends list for real-time updates
     * @return Flow of friend profiles lists
     */
    suspend fun observeFriends(): Flow<List<UserProfile>>
    
    /**
     * Check if a user is a friend of the current user
     * @param userId ID of the user to check
     * @return True if friends, false otherwise
     */
    suspend fun isFriend(userId: String): Boolean
    
    /**
     * Check if there's a pending friend request between the current user and another user
     * @param userId ID of the other user
     * @return The request if found, null otherwise
     */
    suspend fun getPendingRequestWithUser(userId: String): FriendRequest?
    
    /**
     * Remove a friend from the current user's friends list
     * @param friendId ID of the friend to remove
     * @return Success or failure
     */
    suspend fun removeFriend(friendId: String): Boolean
}
