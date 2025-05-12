package com.example.childsafe.data.repository

import com.example.childsafe.data.model.FriendRequest
import com.example.childsafe.data.model.FriendRequestStatus
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.domain.repository.FriendRepository
import com.example.childsafe.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : FriendRepository {

    private val friendRequestsCollection = firestore.collection("friendRequests")
    private val friendshipsCollection = firestore.collection("friendships")

    override suspend fun sendFriendRequest(recipientId: String, message: String): String? {
        val currentUserId = userRepository.getCurrentUserId() ?: return null
        
        // Check if users are already friends
        if (isFriend(recipientId)) {
            Timber.d("Already friends with user $recipientId")
            return null
        }
        
        // Check if there's already a pending request between these users
        val existingRequest = getPendingRequestWithUser(recipientId)
        if (existingRequest != null) {
            Timber.d("Already have a pending request with user $recipientId")
            return existingRequest.requestId
        }
        
        try {
            // Create a new friend request
            val requestData = hashMapOf(
                "senderId" to currentUserId,
                "recipientId" to recipientId,
                "status" to FriendRequestStatus.PENDING.name,
                "message" to message,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            val requestRef = friendRequestsCollection.add(requestData).await()
            return requestRef.id
        } catch (e: Exception) {
            Timber.e(e, "Error sending friend request")
            return null
        }
    }

    override suspend fun getSentFriendRequests(): List<FriendRequest> {
        val currentUserId = userRepository.getCurrentUserId() ?: return emptyList()
        
        return try {
            val snapshot = friendRequestsCollection
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .get()
                .await()
                
            val requests = snapshot.documents.mapNotNull { doc -> 
                doc.toObject(FriendRequest::class.java)?.copy(requestId = doc.id)
            }
            
            // Load recipient profiles
            requests.forEach { request ->
                request.senderProfile = userRepository.getUserProfile(request.recipientId)
            }
            
            requests
        } catch (e: Exception) {
            Timber.e(e, "Error getting sent friend requests")
            emptyList()
        }
    }

    override suspend fun getReceivedFriendRequests(): List<FriendRequest> {
        val currentUserId = userRepository.getCurrentUserId() ?: return emptyList()
        
        return try {
            val snapshot = friendRequestsCollection
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .get()
                .await()
                
            val requests = snapshot.documents.mapNotNull { doc -> 
                doc.toObject(FriendRequest::class.java)?.copy(requestId = doc.id)
            }
            
            // Load sender profiles
            requests.forEach { request ->
                request.senderProfile = userRepository.getUserProfile(request.senderId)
            }
            
            requests
        } catch (e: Exception) {
            Timber.e(e, "Error getting received friend requests")
            emptyList()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun observeReceivedFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val currentUserId = userRepository.getCurrentUserId()
        
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listenerRegistration = friendRequestsCollection
            .whereEqualTo("recipientId", currentUserId)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing received friend requests")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc -> 
                    doc.toObject(FriendRequest::class.java)?.copy(requestId = doc.id)
                } ?: emptyList()
                
                // Load sender profiles for display names, photos, etc.
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        requests.forEach { request ->
                            request.senderProfile = userRepository.getUserProfile(request.senderId)
                        }
                        trySend(requests)
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading profiles for friend requests")
                        trySend(requests) // Send without profiles if we can't load them
                    }
                }
            }
            
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun acceptFriendRequest(requestId: String): Boolean {
        val currentUserId = userRepository.getCurrentUserId() ?: return false
        
        try {
            // Get the request to check it's valid
            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            if (!requestDoc.exists()) return false
            
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return false
                
            // Verify this user is the recipient
            if (request.recipientId != currentUserId) {
                Timber.e("Not authorized to accept this request")
                return false
            }
            
            // Update request status to ACCEPTED
            friendRequestsCollection.document(requestId).update(mapOf(
                "status" to FriendRequestStatus.ACCEPTED.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            
            // Create a friendship entry for both users
            val batch = firestore.batch()
            
            // Entry for current user
            batch.set(friendshipsCollection.document(currentUserId), mapOf(
                "friends" to FieldValue.arrayUnion(request.senderId),
                "updatedAt" to FieldValue.serverTimestamp()
            ), com.google.firebase.firestore.SetOptions.merge())
            
            // Entry for requester
            batch.set(friendshipsCollection.document(request.senderId), mapOf(
                "friends" to FieldValue.arrayUnion(currentUserId),
                "updatedAt" to FieldValue.serverTimestamp()
            ), com.google.firebase.firestore.SetOptions.merge())
            
            batch.commit().await()
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error accepting friend request")
            return false
        }
    }

    override suspend fun rejectFriendRequest(requestId: String, block: Boolean): Boolean {
        val currentUserId = userRepository.getCurrentUserId() ?: return false
        
        try {
            // Get the request to check it's valid
            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            if (!requestDoc.exists()) return false
            
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return false
                
            // Verify this user is the recipient
            if (request.recipientId != currentUserId) {
                Timber.e("Not authorized to reject this request")
                return false
            }
            
            // Update request status to REJECTED or BLOCKED
            val status = if (block) FriendRequestStatus.BLOCKED else FriendRequestStatus.REJECTED
            friendRequestsCollection.document(requestId).update(mapOf(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            
            // If blocking, add to block list (implementation would depend on your blocking system)
            if (block) {
                // Implementation of blocking logic would go here
            }
            
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error rejecting friend request")
            return false
        }
    }

    override suspend fun cancelFriendRequest(requestId: String): Boolean {
        val currentUserId = userRepository.getCurrentUserId() ?: return false
        
        try {
            // Get the request to verify ownership
            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            if (!requestDoc.exists()) return false
            
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return false
                
            // Verify this user is the sender
            if (request.senderId != currentUserId) {
                Timber.e("Not authorized to cancel this request")
                return false
            }
            
            // Delete the request
            friendRequestsCollection.document(requestId).delete().await()
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error canceling friend request")
            return false
        }
    }

    override suspend fun getFriends(): List<UserProfile> {
        val currentUserId = userRepository.getCurrentUserId() ?: return emptyList()
        
        try {
            // Get the user's friendships document
            val friendshipsDoc = friendshipsCollection.document(currentUserId).get().await()
            if (!friendshipsDoc.exists()) return emptyList()
            
            // Get friend IDs
            @Suppress("UNCHECKED_CAST")
            val friendIds = friendshipsDoc.get("friends") as? List<String> ?: emptyList()
            
            // Load each friend's profile
            return friendIds.mapNotNull { friendId ->
                try {
                    userRepository.getUserProfile(friendId)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load profile for friend $friendId")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting friends")
            return emptyList()
        }
    }

    override suspend fun observeFriends(): Flow<List<UserProfile>> = callbackFlow {
        val currentUserId = userRepository.getCurrentUserId()
        
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listenerRegistration = friendshipsCollection.document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing friends")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                @Suppress("UNCHECKED_CAST")
                val friendIds = snapshot.get("friends") as? List<String> ?: emptyList()
                
                // Load friend profiles
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val friends = friendIds.mapNotNull { friendId ->
                            userRepository.getUserProfile(friendId)
                        }
                        trySend(friends)
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading friend profiles")
                        trySend(emptyList())
                    }
                }
            }
            
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun isFriend(userId: String): Boolean {
        val currentUserId = userRepository.getCurrentUserId() ?: return false
        
        try {
            // Get the user's friendships document
            val friendshipsDoc = friendshipsCollection.document(currentUserId).get().await()
            if (!friendshipsDoc.exists()) return false
            
            // Check if userId is in the friends list
            @Suppress("UNCHECKED_CAST")
            val friendIds = friendshipsDoc.get("friends") as? List<String> ?: emptyList()
            
            return friendIds.contains(userId)
        } catch (e: Exception) {
            Timber.e(e, "Error checking friend status")
            return false
        }
    }

    override suspend fun getPendingRequestWithUser(userId: String): FriendRequest? {
        val currentUserId = userRepository.getCurrentUserId() ?: return null
        
        try {
            // Check for requests sent by the current user to userId
            var snapshot = friendRequestsCollection
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
                
            if (!snapshot.isEmpty) {
                return snapshot.documents[0].toObject(FriendRequest::class.java)
                    ?.copy(requestId = snapshot.documents[0].id)
            }
            
            // Check for requests received by the current user from userId
            snapshot = friendRequestsCollection
                .whereEqualTo("senderId", userId)
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name)
                .limit(1)
                .get()
                .await()
                
            if (!snapshot.isEmpty) {
                return snapshot.documents[0].toObject(FriendRequest::class.java)
                    ?.copy(requestId = snapshot.documents[0].id)
            }
            
            return null
        } catch (e: Exception) {
            Timber.e(e, "Error checking for pending friend requests")
            return null
        }
    }

    override suspend fun removeFriend(friendId: String): Boolean {
        val currentUserId = userRepository.getCurrentUserId() ?: return false
        
        try {
            val batch = firestore.batch()
            
            // Remove friendId from current user's friends list
            batch.update(friendshipsCollection.document(currentUserId), 
                "friends", FieldValue.arrayRemove(friendId))
                
            // Remove current user from friendId's friends list
            batch.update(friendshipsCollection.document(friendId), 
                "friends", FieldValue.arrayRemove(currentUserId))
                
            batch.commit().await()
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error removing friend")
            return false
        }
    }
}
