package com.example.childsafe.domain.usecase

import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for searching users
 * Follows clean architecture principles by encapsulating the search functionality
 */
class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Search for users matching the query
     * @param query The search query text
     * @return Flow containing a Result with the list of matching users or an exception
     */
    operator fun invoke(query: String): Flow<Result<List<UserProfile>>> = flow {
        try {
            val results = userRepository.searchUsers(query)
            emit(Result.success(results))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Search for users by email - deprecated as app now uses phone-based authentication
     * This method is kept for compatibility but will return empty results
     * @param email The email to search for
     * @return Flow containing a Result with an empty list
     */
    fun searchByEmail(email: String): Flow<Result<List<UserProfile>>> = flow {
        // Return empty list as email search is no longer supported
        emit(Result.success(emptyList()))
    }
    
    /**
     * Search for users by name
     * @param name The name to search for
     * @return Flow containing a Result with the list of matching users or an exception
     */
    fun searchByName(name: String): Flow<Result<List<UserProfile>>> = flow {
        try {
            val results = userRepository.searchUsersByName(name)
            emit(Result.success(results))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Search for users by phone number
     * @param phoneNumber The phone number to search for
     * @return Flow containing a Result with the list of matching users or an exception
     */
    fun searchByPhone(phoneNumber: String): Flow<Result<List<UserProfile>>> = flow {
        try {
            val results = userRepository.searchUsersByPhone(phoneNumber)
            emit(Result.success(results))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
