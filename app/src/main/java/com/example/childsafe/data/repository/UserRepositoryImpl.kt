package com.example.childsafe.data.repository

import com.example.childsafe.data.model.ProfileVisibility
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.domain.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val documentSnapshot = usersCollection.document(userId).get().await()
            
            if (documentSnapshot.exists()) {
                documentSnapshot.toObject(UserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user profile")
            null
        }
    }

    override suspend fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val listenerRegistration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing user profile")
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val userProfile = if (snapshot?.exists() == true) {
                    snapshot.toObject(UserProfile::class.java)
                } else {
                    null
                }
                
                trySend(userProfile)
            }
        
        awaitClose { 
            listenerRegistration.remove() 
        }
    }

    override suspend fun updateCurrentUserProfile(updates: Map<String, Any>): Boolean {
        val currentUserId = getCurrentUserId() ?: return false
        
        return try {
            usersCollection.document(currentUserId).update(updates).await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error updating user profile")
            false
        }
    }

    // Email search functionality removed as app now uses phone-based authentication

    override suspend fun searchUsersByName(name: String): List<UserProfile> {
        if (name.isBlank()) {
            Timber.d("searchUsersByName: Empty name provided, returning empty list")
            return emptyList()
        }
        
        return try {
            val trimmedName = name.trim()
            Timber.d("searchUsersByName: Starting search with name: '$trimmedName'")
            
            val query = usersCollection
                .whereEqualTo("allowSearchByName", true)
                .orderBy("displayName")
                .startAt(trimmedName)
                .endAt(trimmedName + "\uf8ff") // End with Unicode character after all others
                .limit(20)
                
            Timber.d("searchUsersByName: Executing Firestore query: $query")
            val snapshot = query.get().await()
            Timber.d("searchUsersByName: Firestore query completed, documents count: ${snapshot.size()}")
            
            val results = snapshot.documents.mapNotNull { 
                val profile = it.toObject(UserProfile::class.java)
                if (profile != null) {
                    Timber.d("searchUsersByName: Found user - ID: ${profile.userId}, Name: ${profile.displayName}")
                } else {
                    Timber.w("searchUsersByName: Failed to convert document to UserProfile: ${it.id}")
                }
                profile
            }
            
            Timber.d("searchUsersByName: Search completed, found ${results.size} matching users")
            results
        } catch (e: Exception) {
            Timber.e(e, "searchUsersByName: Error searching users by name - ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    override suspend fun searchUsersByPhone(phoneNumber: String): List<UserProfile> {
        if (phoneNumber.isBlank()) {
            Timber.d("searchUsersByPhone: Empty phone number provided, returning empty list")
            return emptyList()
        }
        
        return try {
            Timber.d("searchUsersByPhone: Starting search for phone: '$phoneNumber'")
            
            // Clean phone number input (remove spaces, dashes, etc)
            val formattedPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
            Timber.d("searchUsersByPhone: Formatted phone for search: '$formattedPhone' (length: ${formattedPhone.length})")
            
            // First try exact match
            val exactMatches = try {
                Timber.d("searchUsersByPhone: Attempting EXACT match search with query: '$formattedPhone'")
                val query = usersCollection
                    .whereEqualTo("allowSearchByPhone", true)
                    .whereEqualTo("phoneNumber", formattedPhone)
                    .limit(20)
                
                Timber.d("searchUsersByPhone: Executing Firestore query: $query")
                val snapshot = query.get().await()
                Timber.d("searchUsersByPhone: Firestore query completed, documents count: ${snapshot.size()}")
                
                val results = snapshot.documents.mapNotNull { 
                    val profile = it.toObject(UserProfile::class.java)
                    if (profile != null) {
                        Timber.d("searchUsersByPhone: Found user - ID: ${profile.userId}, Name: ${profile.displayName}, Phone: ${profile.phoneNumber}")
                    } else {
                        Timber.w("searchUsersByPhone: Failed to convert document to UserProfile: ${it.id}")
                    }
                    profile
                }
                Timber.d("searchUsersByPhone: Exact match search found ${results.size} results")
                results
            } catch (e: Exception) {
                Timber.e(e, "searchUsersByPhone: Error during exact phone match search - ${e.javaClass.simpleName}: ${e.message}")
                emptyList()
            }
            
            if (exactMatches.isNotEmpty()) {
                return exactMatches
            }
            
            // Try suffix match for last digits (common use case)
            if (formattedPhone.length >= 4) {
                Timber.d("searchUsersByPhone: Trying SUFFIX/CONTAINS match for: '$formattedPhone'")
                try {
                    // Get all profiles that allow phone search
                    Timber.d("searchUsersByPhone: Fetching all profiles with allowSearchByPhone=true")
                    val snapshot = usersCollection
                        .whereEqualTo("allowSearchByPhone", true)
                        .limit(100) // Get a reasonable limit for client-side filtering
                        .get()
                        .await()
                    
                    Timber.d("searchUsersByPhone: Fetched ${snapshot.size()} profiles for suffix filtering")
                    
                    // Perform client-side filtering to find phones ending with the search string
                    val suffixMatches = snapshot.documents
                        .mapNotNull { 
                            val profile = it.toObject(UserProfile::class.java)
                            if (profile == null) {
                                Timber.w("searchUsersByPhone: Failed to convert document to UserProfile: ${it.id}")
                            }
                            profile
                        }
                    
                    Timber.d("searchUsersByPhone: Successfully converted ${suffixMatches.size} documents to UserProfile")
                    
                    // Debug log each profile's phone number
                    suffixMatches.forEach {
                        Timber.v("searchUsersByPhone: Checking suffix match - Profile phone: '${it.phoneNumber}', Query: '$formattedPhone'")
                    }
                    
                    // Filter by suffix or contains
                    val matchedProfiles = suffixMatches.filter { 
                        val endsWith = it.phoneNumber.endsWith(formattedPhone)
                        val contains = it.phoneNumber.contains(formattedPhone)
                        
                        if (endsWith || contains) {
                            Timber.d("searchUsersByPhone: MATCH found - ID: ${it.userId}, Name: ${it.displayName}, Phone: '${it.phoneNumber}', " +
                                   "endsWith=$endsWith, contains=$contains")
                        }
                        
                        endsWith || contains
                    }.take(20) // Limit results
                    
                    Timber.d("searchUsersByPhone: Suffix/contains match found ${matchedProfiles.size} results")
                    if (matchedProfiles.isNotEmpty()) {
                        return matchedProfiles
                    }
                } catch (e: Exception) {
                    Timber.e(e, "searchUsersByPhone: Error during suffix match search - ${e.javaClass.simpleName}: ${e.message}")
                }
            }
            
            // If still no matches and search string is at least 3 chars, try a more general search
            if (formattedPhone.length >= 3) {
                Timber.d("searchUsersByPhone: Trying GENERAL DIGIT MATCH search for: '$formattedPhone'")
                try {
                    // Get all profiles that allow phone search (limited number)
                    val snapshot = usersCollection
                        .whereEqualTo("allowSearchByPhone", true)
                        .limit(100)  
                        .get()
                        .await()
                    
                    Timber.d("searchUsersByPhone: Fetched ${snapshot.size()} profiles for general digit matching")
                    
                    // Perform client-side filtering for any part of the phone containing the digits
                    val allProfiles = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                    Timber.d("searchUsersByPhone: Successfully converted ${allProfiles.size} documents to UserProfile for digit matching")
                    
                    // Remove all non-digits from search query for comparison
                    val cleanedSearchPhone = formattedPhone.replace(Regex("[^0-9]"), "")
                    Timber.d("searchUsersByPhone: Cleaned search query for digit-only comparison: '$cleanedSearchPhone'")
                    
                    val results = allProfiles.filter { profile -> 
                        // Remove all non-digits for a more flexible comparison
                        val cleanedProfilePhone = profile.phoneNumber.replace(Regex("[^0-9]"), "")
                        
                        Timber.v("searchUsersByPhone: Comparing - Profile: '$cleanedProfilePhone' with Query: '$cleanedSearchPhone'")
                        val contains = cleanedProfilePhone.contains(cleanedSearchPhone)
                        
                        if (contains) {
                            Timber.d("searchUsersByPhone: DIGIT MATCH found - ID: ${profile.userId}, " +
                                    "Name: ${profile.displayName}, Raw Phone: '${profile.phoneNumber}', " +
                                    "Cleaned Phone: '$cleanedProfilePhone'")
                        }
                        
                        contains
                    }.take(20)
                    
                    Timber.d("searchUsersByPhone: General phone search found ${results.size} results")
                    return results
                } catch (e: Exception) {
                    Timber.e(e, "searchUsersByPhone: Error during general digit match search - ${e.javaClass.simpleName}: ${e.message}")
                    return emptyList()
                }
            }
            
            Timber.d("searchUsersByPhone: No phone matches found with any method")
            return emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error searching users by phone")
            emptyList()
        }
    }

    override suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) {
            Timber.d("searchUsers: Empty query provided, returning empty list")
            return emptyList()
        }
        
        val result = mutableSetOf<UserProfile>()
        Timber.d("searchUsers: Starting general search with query: '$query'")
        
        try {
            // Check if query is mostly numeric (likely a phone number)
            val numericChars = query.replace(Regex("[^0-9+]"), "").length
            val isLikelyPhoneNumber = numericChars >= query.length * 0.7
            Timber.d("searchUsers: Query analysis - Total length: ${query.length}, Numeric chars: $numericChars, Is likely phone: $isLikelyPhoneNumber")
            
            // If it looks like a phone number, prioritize phone search
            if (isLikelyPhoneNumber) {
                Timber.d("searchUsers: Query appears to be a phone number, prioritizing phone search")
                val phoneResults = searchUsersByPhone(query)
                Timber.d("searchUsers: Phone search returned ${phoneResults.size} results")
                result.addAll(phoneResults)
                
                // If we found matches by phone, return them immediately
                if (result.isNotEmpty()) {
                    Timber.d("searchUsers: Returning ${result.size} phone matches as priority results")
                    return result.toList()
                } else {
                    Timber.d("searchUsers: No phone matches found, will try other search methods")
                }
            }
            
            // Otherwise, try all search methods
            
            // Search by name for all queries
            Timber.d("searchUsers: Trying name search for query: '$query'")
            val nameResults = searchUsersByName(query)
            Timber.d("searchUsers: Name search returned ${nameResults.size} results")
            result.addAll(nameResults)
            
            // If not already checked as a likely phone number, check for partial numeric
            if (!isLikelyPhoneNumber && query.contains(Regex("[0-9]"))) {
                Timber.d("searchUsers: Query contains numbers but isn't primarily numeric, trying phone search as fallback")
                val phoneResults = searchUsersByPhone(query)
                Timber.d("searchUsers: Secondary phone search returned ${phoneResults.size} results")
                result.addAll(phoneResults)
            }
            
            Timber.d("searchUsers: Combined search complete, total results: ${result.size}")
        } catch (e: Exception) {
            Timber.e(e, "searchUsers: Error in general user search - ${e.javaClass.simpleName}: ${e.message}")
        }
        
        return result.toList()
    }

    // Email-based user existence check removed as app now uses phone-based authentication
    override suspend fun checkUserExistsByEmail(email: String): Boolean {
        // Always return false since email functionality is removed
        return false
    }

    override suspend fun saveUserProfile(userProfile: UserProfile): Boolean {
        if (userProfile.userId.isBlank()) {
            Timber.e("Cannot save user profile with blank userId")
            return false
        }
        
        return try {
            // Convert to Map for more reliable serialization
            val updates = mapOf(
                "userId" to userProfile.userId,
                "displayName" to userProfile.displayName,
                "phoneNumber" to userProfile.phoneNumber,
                "photoUrl" to userProfile.photoUrl,
                "profileVisibility" to userProfile.profileVisibility.name,
                "allowSearchByPhone" to userProfile.allowSearchByPhone,
                "allowSearchByName" to userProfile.allowSearchByName,
                "isOnline" to userProfile.isOnline,
                "lastActive" to Timestamp.now()
            )
            
            Timber.d("Saving user profile for ${userProfile.userId} with data: $updates")
            
            // Try with SetOptions.merge() for a more forgiving write
            usersCollection.document(userProfile.userId).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            Timber.d("User profile saved successfully for ${userProfile.userId}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving user profile: ${e.message}")
            e.printStackTrace()
            
            try {
                // Try a simpler update as fallback
                Timber.w("Trying fallback method for saving profile")
                val minimalUpdates = mapOf(
                    "displayName" to userProfile.displayName,
                    "phoneNumber" to userProfile.phoneNumber
                )
                usersCollection.document(userProfile.userId).update(minimalUpdates).await()
                Timber.d("Fallback profile save successful")
                true
            } catch (e2: Exception) {
                Timber.e(e2, "Fallback save attempt also failed: ${e2.message}")
                false
            }
        }
    }

    override suspend fun updateOnlineStatus(isOnline: Boolean): Boolean {
        val currentUserId = getCurrentUserId() ?: return false
        
        return try {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastActive" to FieldValue.serverTimestamp()
            )
            
            usersCollection.document(currentUserId).update(updates).await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error updating online status")
            false
        }
    }
    
    override suspend fun createUserAfterPhoneAuth(phoneNumber: String): UserProfile? {
        val currentUser = getCurrentUser() ?: run {
            Timber.e("Cannot create user profile: getCurrentUser returned null")
            return null
        }
        val userId = currentUser.uid
        
        try {
            // Add debug logs
            Timber.d("Attempting to create/update user profile for uid: $userId with phone: $phoneNumber")
            
            // Format the phone number consistently - strip any non-digit or plus sign characters
            val formattedPhoneNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            Timber.d("Formatted phone number: $formattedPhoneNumber")
            
            // Check if user profile already exists
            val existingProfile = getUserProfile(userId)
            if (existingProfile != null) {
                Timber.d("Found existing profile: $existingProfile")
                // If profile exists but phone number is different or empty, update it
                if (existingProfile.phoneNumber != formattedPhoneNumber && formattedPhoneNumber.isNotEmpty()) {
                    Timber.d("Updating phone number from ${existingProfile.phoneNumber} to $formattedPhoneNumber")
                    usersCollection.document(userId).update(
                        mapOf("phoneNumber" to formattedPhoneNumber)
                    ).await()
                    
                    // Return updated profile
                    return existingProfile.copy(phoneNumber = formattedPhoneNumber)
                }
                return existingProfile
            }
            
            Timber.d("No existing profile found, creating new profile")
            // Create a new minimal profile for the user
            val timestamp = com.google.firebase.Timestamp.now()
            // For phone auth, displayName is likely null, so generate a placeholder
            val displayName = "User-${userId.takeLast(5)}"
            
            val newProfile = UserProfile(
                userId = userId,
                displayName = displayName,
                phoneNumber = formattedPhoneNumber,
                photoUrl = currentUser.photoUrl?.toString() ?: "",
                isOnline = true,
                lastActive = timestamp,
                profileVisibility = ProfileVisibility.CONTACTS_ONLY,
                allowSearchByPhone = true,  // Enable search by phone since they signed in with phone
                allowSearchByName = true,   // Enable search by name to help others find them
                createdAt = timestamp
            )
            
            try {
                // Use a much simpler approach with minimal fields to avoid permission issues
                Timber.d("Attempting to save new profile with minimal fields")
                
                // Only use essential fields that should have no permission issues
                val minimalProfileMap = mapOf(
                    "displayName" to newProfile.displayName,
                    "phoneNumber" to formattedPhoneNumber,  // Use the already formatted phone number
                    "isOnline" to true,
                    "lastActive" to com.google.firebase.Timestamp.now()
                )
                
                Timber.d("Using minimal map for Firestore save: $minimalProfileMap")
                
                // Try direct set without any custom options first
                try {
                    // Use the auth user ID directly without any complex objects
                    val docRef = firestore.collection("users").document(userId)
                    docRef.set(minimalProfileMap).await()
                    Timber.d("Direct write with minimal fields successful")
                } catch (e: Exception) {
                    // Log specific error details for debugging
                    Timber.e(e, "Direct write failed with error: ${e.javaClass.simpleName}: ${e.message}")
                    
                    // If that fails, try update() instead of set()
                    try {
                        Timber.w("Trying update() instead")
                        firestore.collection("users").document(userId).update(minimalProfileMap).await()
                        Timber.d("Update method successful")
                    } catch (e2: Exception) {
                        Timber.e(e2, "Update method failed: ${e2.javaClass.simpleName}: ${e2.message}")
                        
                        // Last resort - try one more time with only the most basic field
                        try {
                            Timber.w("Trying last resort with single field")
                            firestore.collection("users").document(userId)
                                .update("phoneNumber", formattedPhoneNumber).await()
                            Timber.d("Last resort update successful")
                        } catch (e3: Exception) {
                            Timber.e(e3, "All methods failed. Last error: ${e3.message}")
                            throw e3
                        }
                    }
                }
                
                Timber.d("Successfully created new user profile for user: $userId")
                
                // Instead of fetching fresh profile, just return the current one 
                // to avoid additional Firestore calls that could fail
                return newProfile
            } catch (e: Exception) {
                Timber.e(e, "Error saving new user profile: ${e.message}")
                // Print stack trace for detailed debugging
                e.printStackTrace()
                return null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating user profile after phone authentication: ${e.message}")
            // Print stack trace for detailed debugging
            e.printStackTrace()
            return null
        }
    }
}
