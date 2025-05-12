package com.example.childsafe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.model.FriendRequest
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.domain.repository.FriendRepository
import com.example.childsafe.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.async
import com.example.childsafe.BuildConfig

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState

    init {
        if (BuildConfig.DEBUG) {
            // In debug mode, use sample data instead of repository data
            loadSampleData()
        } else {
            // In production, use real data from repository
            loadData()
        }
    }
    
    private fun loadSampleData() {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        friends = com.example.childsafe.test.SampleFriendData.friends,
                        receivedRequests = com.example.childsafe.test.SampleFriendData.receivedRequests,
                        sentRequests = com.example.childsafe.test.SampleFriendData.sentRequests
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading sample data")
            }
        }
    }

    private fun loadData() {
        loadFriends()
        loadReceivedRequests()
        loadSentRequests()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingFriends = true, friendsError = null) }
                
                // Start observing friends
                friendRepository.observeFriends().collect { friends ->
                    _uiState.update { 
                        it.copy(
                            friends = friends,
                            isLoadingFriends = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading friends")
                _uiState.update { 
                    it.copy(
                        isLoadingFriends = false,
                        friendsError = "Error loading friends: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadReceivedRequests() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingReceivedRequests = true, receivedRequestsError = null) }
                
                // Start observing received requests
                friendRepository.observeReceivedFriendRequests().collect { requests ->
                    _uiState.update { 
                        it.copy(
                            receivedRequests = requests,
                            isLoadingReceivedRequests = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading received friend requests")
                _uiState.update { 
                    it.copy(
                        isLoadingReceivedRequests = false,
                        receivedRequestsError = "Error loading friend requests: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadSentRequests() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingSentRequests = true, sentRequestsError = null) }
                
                val requests = friendRepository.getSentFriendRequests()
                
                _uiState.update { 
                    it.copy(
                        sentRequests = requests,
                        isLoadingSentRequests = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading sent friend requests")
                _uiState.update { 
                    it.copy(
                        isLoadingSentRequests = false,
                        sentRequestsError = "Error loading sent requests: ${e.message}"
                    )
                }
            }
        }
    }

    fun sendFriendRequest(userId: String, message: String = "") {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSendingRequest = true, actionError = null) }
                
                if (BuildConfig.DEBUG) {
                    // In debug mode, handle sending request using sample data
                    Timber.d("FriendsVM.sendFriendRequest: Using sample data for user: $userId")
                    
                    // Find the user in search results
                    val userToAdd = com.example.childsafe.test.SampleFriendData.searchResults.find { it.userId == userId }
                    
                    if (userToAdd != null) {
                        // Create a new request
                        val newRequestId = "sent_${System.currentTimeMillis()}"
                        val currentUserId = "current_user_id" // Sample data current user
                        
                        val newRequest = FriendRequest(
                            requestId = newRequestId,
                            senderId = currentUserId,
                            recipientId = userId,
                            status = com.example.childsafe.data.model.FriendRequestStatus.PENDING,
                            message = message,
                            createdAt = com.google.firebase.Timestamp.now()
                        )
                        
                        // Add to sent requests
                        val updatedSentRequests = _uiState.value.sentRequests.toMutableList().apply { add(newRequest) }
                        
                        _uiState.update { 
                            it.copy(
                                isSendingRequest = false,
                                successMessage = "Friend request sent",
                                sentRequests = updatedSentRequests
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isSendingRequest = false,
                                actionError = "User not found"
                            )
                        }
                    }
                } else {
                    // In production, use repository
                    val requestId = friendRepository.sendFriendRequest(userId, message)
                    
                    if (requestId != null) {
                        _uiState.update { 
                            it.copy(
                                isSendingRequest = false,
                                successMessage = "Friend request sent"
                            )
                        }
                        
                        // Refresh sent requests
                        loadSentRequests()
                    } else {
                        _uiState.update { 
                            it.copy(
                                isSendingRequest = false,
                                actionError = "Failed to send friend request"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending friend request")
                _uiState.update { 
                    it.copy(
                        isSendingRequest = false,
                        actionError = "Error sending request: ${e.message}"
                    )
                }
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessingAction = true, actionError = null) }
                
                if (BuildConfig.DEBUG) {
                    // In debug mode, handle request acceptance using sample data
                    Timber.d("FriendsVM.acceptFriendRequest: Using sample data for request: $requestId")
                    
                    // Find the request in received requests
                    val request = _uiState.value.receivedRequests.find { it.requestId == requestId }
                    if (request != null) {
                        // Add the sender to friends
                        val senderProfile = request.senderProfile ?: UserProfile(userId = request.senderId)
                        val updatedFriends = _uiState.value.friends.toMutableList().apply { add(senderProfile) }
                        
                        // Remove from received requests
                        val updatedRequests = _uiState.value.receivedRequests.filter { it.requestId != requestId }
                        
                        // Update the UI state
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                successMessage = "Friend request accepted",
                                friends = updatedFriends,
                                receivedRequests = updatedRequests
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                actionError = "Request not found"
                            )
                        }
                    }
                } else {
                    // In production, use repository
                    val success = friendRepository.acceptFriendRequest(requestId)
                    
                    if (success) {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                successMessage = "Friend request accepted"
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                actionError = "Failed to accept friend request"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error accepting friend request")
                _uiState.update { 
                    it.copy(
                        isProcessingAction = false,
                        actionError = "Error accepting request: ${e.message}"
                    )
                }
            }
        }
    }

    fun rejectFriendRequest(requestId: String, block: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessingAction = true, actionError = null) }
                
                if (BuildConfig.DEBUG) {
                    // In debug mode, handle request rejection using sample data
                    Timber.d("FriendsVM.rejectFriendRequest: Using sample data for request: $requestId")
                    
                    // Remove from received requests
                    val updatedRequests = _uiState.value.receivedRequests.filter { it.requestId != requestId }
                    
                    // Did we find and remove the request?
                    if (updatedRequests.size < _uiState.value.receivedRequests.size) {
                        val message = if (block) "Request rejected and user blocked" else "Friend request rejected"
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                successMessage = message,
                                receivedRequests = updatedRequests
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                actionError = "Request not found"
                            )
                        }
                    }
                } else {
                    // In production, use repository
                    val success = friendRepository.rejectFriendRequest(requestId, block)
                    
                    if (success) {
                        val message = if (block) "Request rejected and user blocked" else "Friend request rejected"
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                successMessage = message
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                actionError = "Failed to reject friend request"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error rejecting friend request")
                _uiState.update { 
                    it.copy(
                        isProcessingAction = false,
                        actionError = "Error rejecting request: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessingAction = true, actionError = null) }
                
                val success = friendRepository.cancelFriendRequest(requestId)
                
                if (success) {
                    _uiState.update { 
                        it.copy(
                            isProcessingAction = false,
                            successMessage = "Friend request canceled"
                        )
                    }
                    
                    // Refresh sent requests
                    loadSentRequests()
                } else {
                    _uiState.update { 
                        it.copy(
                            isProcessingAction = false,
                            actionError = "Failed to cancel friend request"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error canceling friend request")
                _uiState.update { 
                    it.copy(
                        isProcessingAction = false,
                        actionError = "Error canceling request: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessingAction = true, actionError = null) }
                
                if (BuildConfig.DEBUG) {
                    // In debug mode, handle friend removal using sample data
                    Timber.d("FriendsVM.removeFriend: Using sample data to remove friend: $friendId")
                    
                    // Remove from friends list
                    val updatedFriends = _uiState.value.friends.filter { it.userId != friendId }
                    
                    // Did we find and remove the friend?
                    if (updatedFriends.size < _uiState.value.friends.size) {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                successMessage = "Friend removed",
                                friends = updatedFriends
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                actionError = "Friend not found"
                            )
                        }
                    }
                } else {
                    // In production, use repository
                    val success = friendRepository.removeFriend(friendId)
                    
                    if (success) {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                successMessage = "Friend removed"
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isProcessingAction = false,
                                actionError = "Failed to remove friend"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing friend")
                _uiState.update { 
                    it.copy(
                        isProcessingAction = false,
                        actionError = "Error removing friend: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(successMessage = null, actionError = null)
        }
    }

    /**
     * Search for users by a query string
     * @param query Text to search for
     * @return List of matching user profiles
     */
    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.length < 3) {
            Timber.d("FriendsVM.searchUsers: Query too short (${query.length} chars), minimum 3 required")
            return emptyList()
        }
        
        Timber.d("FriendsVM.searchUsers: Starting search with query: '$query'")
        
        return try {
            if (BuildConfig.DEBUG) {
                // Use sample data for testing - fixed to avoid potential issues
                Timber.d("FriendsVM.searchUsers: Using SAMPLE DATA for testing with query: '$query'")
                val filteredSamples = com.example.childsafe.test.SampleFriendData.searchResults.filter { 
                    val nameMatch = it.displayName.contains(query, ignoreCase = true)
                    val phoneMatch = it.phoneNumber.contains(query, ignoreCase = true)
                    
                    if (nameMatch || phoneMatch) {
                        Timber.d("FriendsVM.searchUsers: Sample match - Name: ${it.displayName}, Phone: ${it.phoneNumber}, " +
                                "nameMatch=$nameMatch, phoneMatch=$phoneMatch")
                    }
                    
                    nameMatch || phoneMatch
                }
                Timber.d("FriendsVM.searchUsers: Sample data search found ${filteredSamples.size} matching results")
                return filteredSamples
            } else {
                // For production - direct call without using viewModelScope.async
                Timber.d("FriendsVM.searchUsers: Calling repository searchUsers() with query: '$query'")
                val results = userRepository.searchUsers(query)
                Timber.d("FriendsVM.searchUsers: Repository returned ${results.size} raw results")
                
                // Filter out users who are already friends
                val friends = _uiState.value.friends
                Timber.d("FriendsVM.searchUsers: Filtering out ${friends.size} existing friends")
                
                // Debug log all results before filtering
                results.forEach { 
                    val isFriend = friends.any { friend -> friend.userId == it.userId }
                    Timber.d("FriendsVM.searchUsers: Result - ID: ${it.userId}, Name: ${it.displayName}, " +
                           "Phone: ${it.phoneNumber}, IsAlreadyFriend: $isFriend") 
                }
                
                val filteredResults = results.filterNot { searchResult -> 
                    friends.any { it.userId == searchResult.userId }
                }
                
                Timber.d("FriendsVM.searchUsers: Returning ${filteredResults.size} filtered results after excluding friends")
                filteredResults
            }
        } catch (e: Exception) {
            Timber.e(e, "FriendsVM.searchUsers: Error searching for users - ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Search users from the UI thread
     * This method starts a coroutine to perform the search and calls the callback with the results
     * @param query The search query
     * @param onResult Callback that will receive the search results
     */
    fun searchUsersFromUi(query: String, onResult: (List<UserProfile>) -> Unit) {
        if (query.length < 3) {
            Timber.d("FriendsVM.searchUsersFromUi: Query too short (${query.length} chars), minimum 3 required")
            onResult(emptyList())
            return
        }
        
        Timber.d("FriendsVM.searchUsersFromUi: Initiating search from UI with query: '$query'")
        
        viewModelScope.launch {
            try {
                Timber.d("FriendsVM.searchUsersFromUi: Launching coroutine for search")
                val startTime = System.currentTimeMillis()
                
                val results = searchUsers(query)
                
                val duration = System.currentTimeMillis() - startTime
                Timber.d("FriendsVM.searchUsersFromUi: Search completed in ${duration}ms, found ${results.size} results")
                
                // Debug log the results
                if (results.isNotEmpty()) {
                    Timber.d("FriendsVM.searchUsersFromUi: Top ${Math.min(3, results.size)} results:")
                    results.take(3).forEachIndexed { index, profile ->
                        Timber.d("FriendsVM.searchUsersFromUi: Result #${index + 1} - ID: ${profile.userId}, " +
                               "Name: ${profile.displayName}, Phone: ${profile.phoneNumber}")
                    }
                    if (results.size > 3) {
                        Timber.d("FriendsVM.searchUsersFromUi: ... and ${results.size - 3} more results")
                    }
                } else {
                    Timber.d("FriendsVM.searchUsersFromUi: No results found for query: '$query'")
                }
                
                onResult(results)
            } catch (e: Exception) {
                Timber.e(e, "FriendsVM.searchUsersFromUi: Error searching users from UI - ${e.javaClass.simpleName}: ${e.message}")
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Search users specifically by phone number
     * This is helpful when we know we're looking for a phone number match
     * @param phoneNumber The phone number to search for
     * @param onResult Callback that will receive the search results
     */
    fun searchUsersByPhoneFromUi(phoneNumber: String, onResult: (List<UserProfile>) -> Unit) {
        // Allow shorter queries for phone search since they may just enter last digits
        if (phoneNumber.length < 2) {
            Timber.d("FriendsVM.searchUsersByPhoneFromUi: Query too short (${phoneNumber.length} chars), minimum 2 required for phone search")
            onResult(emptyList())
            return
        }
        
        Timber.d("FriendsVM.searchUsersByPhoneFromUi: Initiating PHONE-SPECIFIC search from UI with query: '$phoneNumber'")
        
        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // Format phone number to remove any unnecessary characters
                val formattedPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
                Timber.d("FriendsVM.searchUsersByPhoneFromUi: Formatted phone search query: '$formattedPhone' (length: ${formattedPhone.length})")
                
                // Get current user for filtering
                val currentUserId = userRepository.getCurrentUserId()
                Timber.d("FriendsVM.searchUsersByPhoneFromUi: Current user ID for filtering: $currentUserId")
                
                // Use sample data in debug mode
                val rawResults = if (BuildConfig.DEBUG) {
                    Timber.d("FriendsVM.searchUsersByPhoneFromUi: Using SAMPLE DATA for testing with query: '$formattedPhone'")
                    // Filter sample data by phone number
                    val filteredSamples = com.example.childsafe.test.SampleFriendData.searchResults.filter { 
                        it.phoneNumber.contains(formattedPhone, ignoreCase = true)
                    }
                    Timber.d("FriendsVM.searchUsersByPhoneFromUi: Sample data search found ${filteredSamples.size} matching results by phone")
                    filteredSamples
                } else {
                    // Perform the repository search for production
                    Timber.d("FriendsVM.searchUsersByPhoneFromUi: Calling repository searchUsersByPhone()")
                    userRepository.searchUsersByPhone(formattedPhone)
                }
                val repoSearchTime = System.currentTimeMillis() - startTime
                Timber.d("FriendsVM.searchUsersByPhoneFromUi: Repository search completed in ${repoSearchTime}ms, found ${rawResults.size} raw results")
                
                // Remove self from results
                val resultsWithoutSelf = rawResults.filter { it.userId != currentUserId }
                if (rawResults.size != resultsWithoutSelf.size) {
                    Timber.d("FriendsVM.searchUsersByPhoneFromUi: Removed self from results (filtered out ${rawResults.size - resultsWithoutSelf.size} entries)")
                }
                
                // Debug log the raw results
                if (resultsWithoutSelf.isNotEmpty()) {
                    Timber.d("FriendsVM.searchUsersByPhoneFromUi: Results after self-filtering:")
                    resultsWithoutSelf.forEachIndexed { index, profile ->
                        Timber.d("FriendsVM.searchUsersByPhoneFromUi: Result #${index + 1} - ID: ${profile.userId}, " +
                               "Name: '${profile.displayName}', Phone: '${profile.phoneNumber}'")
                    }
                }
                
                // Filter out existing friends and pending requests
                val filteredResults = resultsWithoutSelf.filter { profile ->
                    val isFriendResult = isFriend(profile.userId)
                    val hasPendingRequest = getPendingRequestWithUser(profile.userId) != null
                    
                    if (isFriendResult) {
                        Timber.d("FriendsVM.searchUsersByPhoneFromUi: Excluding user ${profile.userId} ('${profile.displayName}') - Already a friend")
                    }
                    
                    if (hasPendingRequest) {
                        val request = getPendingRequestWithUser(profile.userId)
                        val direction = if (request?.senderId == currentUserId) "outgoing" else "incoming"
                        Timber.d("FriendsVM.searchUsersByPhoneFromUi: Excluding user ${profile.userId} ('${profile.displayName}') - Has $direction friend request")
                    }
                    
                    !isFriendResult && !hasPendingRequest
                }
                
                val totalTime = System.currentTimeMillis() - startTime
                Timber.d("FriendsVM.searchUsersByPhoneFromUi: Search pipeline complete in ${totalTime}ms - Returning ${filteredResults.size} filtered results")
                
                onResult(filteredResults)
            } catch (e: Exception) {
                Timber.e(e, "FriendsVM.searchUsersByPhoneFromUi: Error searching users by phone from UI - ${e.javaClass.simpleName}: ${e.message}")
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Check if a user is already a friend
     * @param userId The user ID to check
     * @return True if they are a friend, false otherwise
     */
    suspend fun isFriend(userId: String): Boolean {
        if (BuildConfig.DEBUG) {
            // Check sample data
            return com.example.childsafe.test.SampleFriendData.friends.any { it.userId == userId }
        }
        
        return try {
            friendRepository.isFriend(userId)
        } catch (e: Exception) {
            Timber.e(e, "Error checking if user is a friend")
            false
        }
    }
    
    /**
     * Check if there's a pending friend request with a user
     * @param userId The user ID to check
     * @return The request if found, null otherwise
     */
    suspend fun getPendingRequestWithUser(userId: String): FriendRequest? {
        if (BuildConfig.DEBUG) {
            // Check sample data
            val sentRequest = com.example.childsafe.test.SampleFriendData.sentRequests.find { it.recipientId == userId }
            if (sentRequest != null) return sentRequest
            
            val receivedRequest = com.example.childsafe.test.SampleFriendData.receivedRequests.find { it.senderId == userId }
            if (receivedRequest != null) return receivedRequest
            
            return null
        }
        
        return try {
            friendRepository.getPendingRequestWithUser(userId)
        } catch (e: Exception) {
            Timber.e(e, "Error checking pending requests")
            null
        }
    }

    /**
     * Start a chat with a friend
     * This method is meant to be used by the UI to initiate a chat with a friend
     * @param friendId ID of the friend to chat with
     * @return The conversation ID if successful, null otherwise
     */
    fun startChatWithFriend(friendId: String, onComplete: (conversationId: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessingAction = true) }
                
                // First check if the user is a friend
                val isFriend = isFriend(friendId)
                if (!isFriend) {
                    _uiState.update { 
                        it.copy(
                            isProcessingAction = false,
                            actionError = "You can only chat with friends"
                        )
                    }
                    onComplete(null)
                    return@launch
                }
                
                // In a real implementation, you would create or retrieve an existing conversation
                // For now, we'll just simulate success by returning the friend ID as the conversation ID
                _uiState.update { it.copy(isProcessingAction = false) }
                onComplete(friendId)
                
            } catch (e: Exception) {
                Timber.e(e, "Error starting chat with friend")
                _uiState.update { 
                    it.copy(
                        isProcessingAction = false,
                        actionError = "Error starting chat: ${e.message}"
                    )
                }
                onComplete(null)
            }
        }
    }
}

data class FriendsUiState(
    val friends: List<UserProfile> = emptyList(),
    val receivedRequests: List<FriendRequest> = emptyList(),
    val sentRequests: List<FriendRequest> = emptyList(),
    
    val isLoadingFriends: Boolean = false,
    val isLoadingReceivedRequests: Boolean = false,
    val isLoadingSentRequests: Boolean = false,
    val isProcessingAction: Boolean = false,
    val isSendingRequest: Boolean = false,
    
    val friendsError: String? = null,
    val receivedRequestsError: String? = null,
    val sentRequestsError: String? = null,
    val actionError: String? = null,
    val successMessage: String? = null
)
