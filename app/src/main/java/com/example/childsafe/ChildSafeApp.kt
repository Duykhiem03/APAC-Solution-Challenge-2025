package com.example.childsafe

import android.app.Application
import com.example.childsafe.services.ChatNotificationService
import com.example.childsafe.services.FirebaseMessagingManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main application class that initializes Dagger Hilt and other app-wide components
 */
@HiltAndroidApp
class ChildSafeApp : Application() {
    
    @Inject
    lateinit var firebaseMessagingManager: FirebaseMessagingManager
    
    @Inject
    lateinit var chatNotificationService: ChatNotificationService
    
    // Application scope for coroutines
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize Firebase Cloud Messaging
        initFirebaseMessaging()
        
        // Initialize notification channels
        chatNotificationService.createNotificationChannels()
    }
    
    /**
     * Initialize Firebase Cloud Messaging
     * This will request and store the FCM token for push notifications
     */
    private fun initFirebaseMessaging() {
        // Set automatic initialization enabled
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        
        // Get and register FCM token
        applicationScope.launch {
            try {
                firebaseMessagingManager.initializeToken()
                
                // Optional: Log the token for debugging purposes
                if (BuildConfig.DEBUG) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Timber.d("FCM token: ${task.result}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize FCM token")
            }
        }
    }
}




