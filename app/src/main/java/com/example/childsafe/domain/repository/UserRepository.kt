package com.example.childsafe.domain.repository

import com.example.childsafe.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import com.google.firebase.auth.FirebaseUser

/**
 * Repository interface for user-related operations
 */
interface UserRepository {

    /**
     * Get the current logged-in Firebase user
     * @return Current FirebaseUser or null if not logged in
     */
    fun getCurrentUser(): FirebaseUser?
    
    /**
     * Get the current user's ID
     * @return Current user ID or null if not logged in
     */
    fun getCurrentUserId(): String?
    
    /**
     * Get user profile by ID
     * @param userId The user ID
     * @return UserProfile if found, null otherwise
     */
    suspend fun getUserProfile(userId: String): UserProfile?
    
    /**
     * Observe a user's profile for real-time updates
     * @param userId The user ID
     * @return Flow of UserProfile updates
     */
    suspend fun observeUserProfile(userId: String): Flow<UserProfile?>
    
    /**
     * Update current user's profile
     * @param updates Map of fields to update
     * @return Success or failure
     */
    suspend fun updateCurrentUserProfile(updates: Map<String, Any>): Boolean

    
    /**
     * Search for users by name
     * @param name Name to search for
     * @return List of matching user profiles
     */
    suspend fun searchUsersByName(name: String): List<UserProfile>
    
    /**
     * Search for users by phone number
     * @param phoneNumber Phone number to search for
     * @return List of matching user profiles
     */
    suspend fun searchUsersByPhone(phoneNumber: String): List<UserProfile>
    
    /**
     * Search for users with a general query that checks multiple fields
     * @param query Text to search for in various user fields
     * @return List of matching user profiles
     */
    suspend fun searchUsers(query: String): List<UserProfile>
    
    /**
     * Check if a user exists with the given email
     * @param email Email to check
     * @return True if user exists, false otherwise
     */
    suspend fun checkUserExistsByEmail(email: String): Boolean
    
    /**
     * Create or update a user profile
     * @param userProfile UserProfile object to save
     * @return Success or failure
     */
    suspend fun saveUserProfile(userProfile: UserProfile): Boolean
    
    /**
     * Create a new user profile after authentication with phone number
     * This creates a minimal profile which can be updated later
     * @param phoneNumber The authenticated phone number
     * @return The created UserProfile or null if creation failed
     */
    suspend fun createUserAfterPhoneAuth(phoneNumber: String): UserProfile?
    
    /**
     * Update user's online status
     * @param isOnline Whether the user is online
     * @return Success or failure
     */
    suspend fun updateOnlineStatus(isOnline: Boolean): Boolean
}
