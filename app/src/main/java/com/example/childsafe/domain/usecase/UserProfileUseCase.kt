package com.example.childsafe.domain.usecase

import com.example.childsafe.data.model.ProfileVisibility
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for managing user profiles
 */
class UserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Get user profile by ID
     * @param userId The user ID
     * @return Flow containing a Result with the user profile or an exception
     */
    fun getUserProfile(userId: String): Flow<Result<UserProfile?>> = flow {
        try {
            val profile = userRepository.getUserProfile(userId)
            emit(Result.success(profile))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get current user's profile
     * @return Flow containing a Result with the current user's profile or an exception
     */
    fun getCurrentUserProfile(): Flow<Result<UserProfile?>> = flow {
        try {
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {
                val profile = userRepository.getUserProfile(userId)
                emit(Result.success(profile))
            } else {
                emit(Result.success(null))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Update user profile visibility settings
     * @param visibility The new profile visibility setting
     * @param allowSearchByName Whether to allow searching for the user by name
     * @param allowSearchByPhone Whether to allow searching for the user by phone
     * @return Flow containing a Result with success status or an exception
     */
    fun updatePrivacySettings(
        visibility: ProfileVisibility,
        allowSearchByName: Boolean,
        allowSearchByPhone: Boolean
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = mapOf(
                "profileVisibility" to visibility.name,
                "allowSearchByName" to allowSearchByName,
                "allowSearchByPhone" to allowSearchByPhone
            )
            
            val result = userRepository.updateCurrentUserProfile(updates)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Update basic profile information
     * @param displayName The user's display name
     * @param photoUrl The URL to the user's profile photo
     * @param phoneNumber The user's phone number
     * @return Flow containing a Result with success status or an exception
     */
    fun updateProfileInfo(
        displayName: String? = null,
        photoUrl: String? = null,
        phoneNumber: String? = null
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = mutableMapOf<String, Any>()
            
            displayName?.let { updates["displayName"] = it }
            photoUrl?.let { updates["photoUrl"] = it }
            phoneNumber?.let { updates["phoneNumber"] = it }
            
            if (updates.isEmpty()) {
                emit(Result.success(true))
                return@flow
            }
            
            val result = userRepository.updateCurrentUserProfile(updates)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Update online status
     * @param isOnline Whether the user is online
     * @return Flow containing a Result with success status or an exception
     */
    fun updateOnlineStatus(isOnline: Boolean): Flow<Result<Boolean>> = flow {
        try {
            val result = userRepository.updateOnlineStatus(isOnline)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
