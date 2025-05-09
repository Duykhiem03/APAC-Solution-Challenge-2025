package com.example.childsafe.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.childsafe.MainActivity
import com.example.childsafe.R
import com.example.childsafe.utils.ConversationReadEvent
import com.example.childsafe.utils.EventBusManager
import com.example.childsafe.utils.StatusUpdateEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Service class to handle Firebase Cloud Messaging (FCM)
 * This service handles:
 * 1. Token refreshes
 * 2. Notification messages when app is in foreground
 * 3. Data messages for both foreground and background
 */
@AndroidEntryPoint
class ChildSafeMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    @Inject
    lateinit var chatNotificationService: ChatNotificationService
    
    // Create a CoroutineScope for the service
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    // Create a lazy-initialized messaging manager
    private val messagingManager by lazy {
        FirebaseMessagingManager(auth, firestore)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels
        chatNotificationService.createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendRegistrationTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle FCM messages here.
        Timber.d("From: ${remoteMessage.from}")

        // First check if this is a data message
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Message data payload: ${remoteMessage.data}")
            
            // Process data message based on type
            processDataMessage(remoteMessage)
        } else {
            // If there's no data payload, process as a general notification
            processGeneralNotification(remoteMessage)
        }
    }

    /**
     * Process a general (non-chat) notification
     */
    private fun processGeneralNotification(remoteMessage: RemoteMessage) {
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: ${it.body}")
            sendNotification(it.body)
        }
    }

    /**
     * Process FCM data messages
     */
    private fun processDataMessage(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) return
        
        when (data["type"]) {
            "chat_message" -> {
                handleChatMessage(data)
            }
            "status_update" -> {
                handleStatusUpdate(data)
            }
            else -> {
                processGeneralNotification(remoteMessage)
            }
        }
    }
    
    /**
     * Handle messages about delivery status changes
     */
    private fun handleStatusUpdate(data: Map<String, String>) {
        serviceScope.launch {
            try {
                // Get required parameters
                val notificationType = data["notificationType"] ?: "message_status_change"
                
                when (notificationType) {
                    "message_status_change" -> {
                        val messageId = data["messageId"] ?: return@launch
                        val newStatus = data["newStatus"] ?: return@launch
                        
                        // Update local database
                        val messageDeliveryService = FirebaseServiceLocator.getMessageDeliveryService()
                        messageDeliveryService.handleStatusUpdateNotification(messageId, newStatus)
                        
                        // Broadcast the update to active ViewModels
                        EventBusManager.post(StatusUpdateEvent(messageId, newStatus))
                    }
                    "conversation_read" -> {
                        // Handle bulk read notifications
                        val conversationId = data["conversationId"] ?: return@launch
                        val readerId = data["readerId"] ?: return@launch
                        val readerName = data["readerName"] ?: "Someone"
                        
                        // Broadcast conversation read event
                        EventBusManager.post(ConversationReadEvent(conversationId, readerId, readerName))
                    }
                    else -> {
                        Timber.w("Unknown notification type: $notificationType")
                    }
                }
                
                // We don't show notifications for status updates, they're silent
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process status update")
            }
        }
    }
    
    /**
     * Handle chat message notifications
     */
    private fun handleChatMessage(data: Map<String, String>) {
        // Try to handle as chat message
        val handledAsChat = chatNotificationService.handleChatMessageNotification(data)
        
        // If it wasn't handled as a chat, process as general notification
        if (!handledAsChat) {
            val body = data["messageText"] ?: "New message"
            sendNotification(body)
        }
    }

    /**
     * Store FCM token in server/Firestore for sending targeted notifications
     */
    private fun sendRegistrationTokenToServer(token: String) {
        serviceScope.launch {
            try {
                messagingManager.registerTokenWithServer(token)
            } catch (e: Exception) {
                Timber.e(e, "Failed to register token with server")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun sendNotification(messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(getString(R.string.fcm_message))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}