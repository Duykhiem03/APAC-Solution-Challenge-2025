package com.example.childsafe

import android.app.Application
import androidx.work.Configuration
import com.example.childsafe.services.ChatNotificationService
import com.example.childsafe.services.FirebaseMessagingManager
import com.example.childsafe.services.FirebaseServiceLocator
import com.example.childsafe.services.MessageDeliveryService
import com.example.childsafe.services.MessageSyncService
import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
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
class ChildSafeApp() : Application(), Configuration.Provider {
    
    @Inject
    lateinit var firebaseMessagingManager: FirebaseMessagingManager
      @Inject
    lateinit var chatNotificationService: ChatNotificationService

    @Inject
    lateinit var messageDeliveryServiceProvider: javax.inject.Provider<MessageDeliveryService>
    
    @Inject
    lateinit var messageSyncService: MessageSyncService
    
    @Inject
    lateinit var authRepository: com.example.childsafe.auth.FirebaseAuthRepository
      @Inject
    override lateinit var workManagerConfiguration: Configuration
    
    @Inject
    lateinit var buildConfig: BuildConfigStrategy
    
    // Application scope for coroutines
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (buildConfig.isDebug) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize service locator for Firebase components
        FirebaseServiceLocator.setMessageDeliveryServiceProvider(messageDeliveryServiceProvider)
        
        // Initialize Firebase Cloud Messaging        initFirebaseMessaging()
          // Initialize notification channels
        chatNotificationService.createNotificationChannels()
        
        // Start message synchronization service for offline messages
        messageSyncService.start()
        
        // Schedule periodic retries for failed messages
        messageSyncService.schedulePeriodicRetries()
        
        // Set up authentication state listener
        authRepository.setupAuthStateListener()
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
                if (buildConfig.isDebug) {
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




