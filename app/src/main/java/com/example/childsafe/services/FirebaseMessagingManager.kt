package com.example.childsafe.services

import com.example.childsafe.data.model.UserFcmToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for Firebase Cloud Messaging
 * Handles token management and registration
 */
@Singleton
class FirebaseMessagingManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val fcmTokensCollection = firestore.collection("userFcmTokens")
    
    /**
     * Gets the current FCM token and registers it with the server
     * Call this method from the Application class or a ViewModel's init
     */
    suspend fun initializeToken() {
        try {
            val token = getFirebaseToken()
            registerTokenWithServer(token)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize FCM token")
        }
    }

    /**
     * Registers the token with Firestore
     * Associates the token with the current user
     */
    suspend fun registerTokenWithServer(token: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        try {
            withContext(Dispatchers.IO) {
                // Create or update FCM token entry for this user
                val tokenData = UserFcmToken(
                    userId = currentUserId,
                    token = token,
                    lastUpdated = FieldValue.serverTimestamp(),
                    deviceInfo = android.os.Build.MODEL
                )
                
                // We'll use a token-specific document ID to support multiple devices per user
                val tokenDocId = "${currentUserId}_${token.takeLast(10)}"
                
                // Store in Firestore and handle errors
                fcmTokensCollection.document(tokenDocId).set(tokenData)
                    .addOnSuccessListener { 
                        Timber.d("FCM token successfully registered") 
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Failed to register FCM token with server")
                    }
                    .await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error registering FCM token")
        }
    }

    /**
     * Removes a token from the server when user logs out
     * or when the token is no longer valid
     */
    suspend fun unregisterCurrentToken() {
        try {
            val token = getFirebaseToken()
            val currentUserId = auth.currentUser?.uid ?: return
            
            val tokenDocId = "${currentUserId}_${token.takeLast(10)}"
            withContext(Dispatchers.IO) {
                fcmTokensCollection.document(tokenDocId).delete().await()
                Timber.d("FCM token unregistered from server")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister FCM token")
        }
    }

    /**
     * Gets the current FCM token
     */
    private suspend fun getFirebaseToken(): String {
        return withContext(Dispatchers.IO) {
            try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                Timber.e(e, "Error getting FCM token")
                throw e
            }
        }
    }
}