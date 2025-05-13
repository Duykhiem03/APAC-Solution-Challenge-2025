package com.example.childsafe.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.childsafe.MainActivity
import com.example.childsafe.R
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for handling and displaying chat notifications.
 * This service integrates with Firebase Cloud Messaging to deliver real-time
 * message notifications with rich features like direct reply and conversation styling.
 */
@Singleton
class ChatNotificationService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        // Regular chat messages channel
        private const val CHAT_CHANNEL_ID = "chat_notifications"
        
        // Important messages channel (for high priority messages)
        private const val IMPORTANT_CHANNEL_ID = "important_notifications"
        
        // SOS/Emergency messages channel (highest priority)
        private const val SOS_CHANNEL_ID = "sos_notifications"
        
        // Group key for bundling notifications
        private const val GROUP_KEY_CHATS = "com.example.childsafe.CHATS"
        
        // Request codes for PendingIntents
        private const val REQUEST_CODE_CHAT = 0
        private const val REQUEST_CODE_REPLY = 1
        
        // Key for remote input (replies)
        private const val KEY_TEXT_REPLY = "key_text_reply"
        
        // ID for summary notification
        private const val SUMMARY_ID = 0
        
        // Maximum messages to show per conversation
        private const val MAX_MESSAGES_PER_CONVERSATION = 10
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // Coroutine scope for background work
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    // Cache for recent messages (for conversation style)
    private val conversationMessages = ConcurrentHashMap<String, MutableList<Message>>()
    
    /**
     * Initialize notification channels required for chat notifications
     */
    fun createNotificationChannels() {
        // Regular chat channel (default priority)
        val chatChannel = NotificationChannel(
            CHAT_CHANNEL_ID,
            context.getString(R.string.channel_chat_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_chat_description)
            setShowBadge(true)
            enableVibration(true)
            lockscreenVisibility = 0 // NotificationChannel.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(chatChannel)

        // Important messages channel (high priority)
        val importantChannel = NotificationChannel(
            IMPORTANT_CHANNEL_ID,
            context.getString(R.string.channel_important_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_important_description)
            setShowBadge(true)
            enableVibration(true)
            lockscreenVisibility = 0 // NotificationChannel.VISIBILITY_PRIVATE
            vibrationPattern = longArrayOf(0, 250, 250, 250) // Custom pattern
        }
        notificationManager.createNotificationChannel(importantChannel)

        // SOS/Emergency channel (highest priority)
        val sosChannel = NotificationChannel(
            SOS_CHANNEL_ID,
            context.getString(R.string.channel_sos_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_sos_description)
            setShowBadge(true)
            enableVibration(true)
            lockscreenVisibility = 1 // NotificationChannel.VISIBILITY_PUBLIC - Show on lock screen
            vibrationPattern = longArrayOf(0, 500, 250, 500) // Distinctive pattern
            setBypassDnd(true) // Bypass Do Not Disturb mode
        }
        notificationManager.createNotificationChannel(sosChannel)
        Timber.d("Chat notification channels created")
    }

    /**
     * Show a notification for a new chat message
     * 
     * @param message The new message to display in the notification
     * @param conversation The conversation this message belongs to
     * @param senderName Name of the message sender
     * @param senderAvatarUrl Profile picture URL of the sender (optional)
     */
    fun showChatMessageNotification(
        message: Message,
        conversation: Conversation,
        senderName: String,
        senderAvatarUrl: String? = null,
        channelId: String = CHAT_CHANNEL_ID
    ) {
        // Generate a unique notification ID based on conversation ID to allow for proper grouping
        val notificationId = conversation.id.hashCode()
        
        // Store message in our conversation cache for conversation style
        val conversationCache = conversationMessages.getOrPut(conversation.id) { mutableListOf() }
        conversationCache.add(message)
        
        // Limit number of cached messages
        if (conversationCache.size > MAX_MESSAGES_PER_CONVERSATION) {
            conversationCache.removeAt(0)
        }
        
        // Create an Intent to open the chat when notification is tapped
        val chatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openChat", true)
            putExtra("conversationId", conversation.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CHAT,
            chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create notification based on message type
        val messageContent = when(message.getMessageTypeEnum()) {
            com.example.childsafe.data.model.MessageType.TEXT -> message.text
            com.example.childsafe.data.model.MessageType.IMAGE -> "📷 Image"
            com.example.childsafe.data.model.MessageType.AUDIO -> "🎤 Voice message"
            com.example.childsafe.data.model.MessageType.LOCATION -> "📍 Location"
            else -> {
                Timber.e("Unknown message type: ${message.messageType}")
                return
            }
        }
        
        // Add reply action if supported
        val replyAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(context.getString(R.string.reply))
                .build()

            val replyIntent = Intent(context, ChatNotificationReceiver::class.java).apply {
                action = ChatNotificationReceiver.ACTION_REPLY
                putExtra("conversationId", conversation.id)
                putExtra("senderName", senderName)
            }

            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REPLY,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            NotificationCompat.Action.Builder(
                R.drawable.ic_reply,
                context.getString(R.string.reply),
                replyPendingIntent
            )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
        } else {
            null
        }

        // Create person objects for conversation style
        val sender = Person.Builder()
            .setName(senderName)
            .setIcon(IconCompat.createWithResource(context, R.drawable.avatar_default))
            .setKey(message.sender)
            .setImportant(true)
            .build()
            
        // Create messaging style notification for conversation appearance
        val messagingStyle = NotificationCompat.MessagingStyle(sender)
            .setConversationTitle(if (conversation.isGroup) conversation.groupName else null)
            .setGroupConversation(conversation.isGroup)
            .addMessage(
                messageContent.toString(),
                message.timestamp.toDate().time,
                sender
            )
            
        // Create the notification builder with the specified channel ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setPriority(
                when (channelId) {
                    SOS_CHANNEL_ID -> NotificationCompat.PRIORITY_MAX
                    IMPORTANT_CHANNEL_ID -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_CHATS)
            .setWhen(message.timestamp.toDate().time)
            .setShowWhen(true)
            
        // Add actions if available
        if (replyAction != null) {
            builder.addAction(replyAction)
        }
        
        // Add mark as read action
        val markReadIntent = Intent(context, ChatNotificationReceiver::class.java).apply {
            action = ChatNotificationReceiver.ACTION_MARK_READ
            putExtra("conversationId", conversation.id)
            putExtra("messageId", message.id)
        }
        
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 
            message.id.hashCode(), // Use message ID for unique request code 
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.addAction(
            R.drawable.ic_check,
            context.getString(R.string.mark_read),
            markReadPendingIntent
        )
        
        // Set sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(defaultSoundUri)
        
        // If we have cached messages, use messaging style
        if (conversationCache.isNotEmpty()) {
            // Ensure user name is never empty for NotificationCompat.MessagingStyle
            val currentUserName = auth.currentUser?.displayName?.takeIf { it.isNotEmpty() } ?: "Me"
            
            // Try to load avatar if URL is provided
            if (senderAvatarUrl != null) {
                serviceScope.launch {
                    try {
                        val avatar = NotificationUtils.loadAvatarFromUrl(senderAvatarUrl)
                        val styledBuilder = NotificationUtils.createMessagingStyleNotification(
                            context,
                            builder,
                            currentUserName,
                            conversationCache,
                            senderName,
                            avatar
                        )
                        
                        // Post the notification with messaging style
                        postNotificationWithPermissionCheck(notificationId, styledBuilder.build())
                    } catch (e: Exception) {
                        Timber.e(e, "Error creating styled notification")
                        // Fallback to basic notification
                        postNotificationWithPermissionCheck(notificationId, builder.build())
                    }
                }
            } else {
                // Use messaging style without avatar
                val styledBuilder = NotificationUtils.createMessagingStyleNotification(
                    context,
                    builder,
                    currentUserName,
                    conversationCache,
                    senderName
                )
                
                // Post the notification
                postNotificationWithPermissionCheck(notificationId, styledBuilder.build())
            }
        } else {
            // Post a basic notification if no cached messages
            postNotificationWithPermissionCheck(notificationId, builder.build())
        }
        
        // Create or update the summary notification for the group
        showChatSummaryNotification()
    }
    
    /**
     * Shows a summary notification for grouped chat messages
     */
    private fun showChatSummaryNotification() {
        val summaryNotification = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.new_messages))
            .setContentText(context.getString(R.string.you_have_new_messages))
            .setSmallIcon(R.drawable.ic_notification)
            .setGroup(GROUP_KEY_CHATS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        postNotificationWithPermissionCheck(SUMMARY_ID, summaryNotification)
    }
    
    /**
     * Handle incoming chat message from Firebase Cloud Messaging
     * 
     * @param data Notification data payload
     * @return true if the notification was handled, false otherwise
     */
    fun handleChatMessageNotification(data: Map<String, String>): Boolean {
        try {
            // Check if this is a chat message notification
            val notificationType = data["type"] ?: return false
            if (notificationType != "chat_message") return false
            
            val conversationId = data["conversationId"] ?: return false
            val messageId = data["messageId"] ?: return false
            val senderId = data["senderId"] ?: return false
            val senderName = data["senderName"]?.takeIf { it.isNotEmpty() } ?: "Unknown"
            val messageText = data["messageText"] ?: ""
            val messageType = data["messageType"] ?: "text"
            val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
            val senderAvatarUrl = data["senderAvatarUrl"]
            
            // Check if the current user is the sender (no notification needed)
            val currentUserId = auth.currentUser?.uid ?: return false
            if (currentUserId == senderId) return true
            
            // Create Message and Conversation objects
            val message = Message(
                id = messageId,
                conversationId = conversationId,
                sender = senderId,
                text = messageText,
                timestamp = com.google.firebase.Timestamp(Date(timestamp)),
                messageType = when(messageType) {
                    "image" -> com.example.childsafe.data.model.MessageType.IMAGE
                    "audio" -> com.example.childsafe.data.model.MessageType.AUDIO
                    "location" -> com.example.childsafe.data.model.MessageType.LOCATION
                    else -> com.example.childsafe.data.model.MessageType.TEXT
                }.toString(),
                read = false
            )
            
            // Fetch conversation details from Firestore
            firestore.collection("conversations").document(conversationId).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val conversation = documentSnapshot.toObject(Conversation::class.java)?.copy(id = documentSnapshot.id)
                        if (conversation != null) {
                            // Determine the appropriate notification channel
                            val channelId = determineNotificationChannel(message, data)
                            
                            // Show the notification
                            showChatMessageNotification(
                                message = message,
                                conversation = conversation,
                                senderName = senderName,
                                senderAvatarUrl = senderAvatarUrl,
                                channelId = channelId
                            )
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "Error fetching conversation for notification")
                }
            
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error handling chat notification")
            return false
        }
    }
    
    /**
     * Cancel all chat notifications for a specific conversation
     */
    fun cancelChatNotifications(conversationId: String) {
        // Clear any cached messages
        conversationMessages.remove(conversationId)
        
        // Cancel the notification
        notificationManager.cancel(conversationId.hashCode())
        
        // If no more notifications, cancel the summary too
        if (conversationMessages.isEmpty()) {
            notificationManager.cancel(SUMMARY_ID)
        }
    }
    
    /**
     * Cancels all chat notifications
     */
    fun cancelAllChatNotifications() {
        // Clear all cached messages
        conversationMessages.clear()
        
        // Cancel the summary notification
        notificationManager.cancel(SUMMARY_ID)
    }

    /**
     * Load recent message history for conversation-style notifications
     * This makes notifications more useful by showing context
     * 
     * @param conversationId The conversation ID to load history for
     * @param currentMessageId The ID of the current message being displayed (to avoid duplicates)
     * @return MessagingStyle with message history
     */
    private fun loadMessageHistory(
        conversationId: String, 
        currentMessageId: String,
        messagingStyle: NotificationCompat.MessagingStyle
    ) {
        try {
            // Start a background job to load message history
            serviceScope.launch {
                val messages = firestore.collection("messages")
                    .whereEqualTo("conversationId", conversationId)
                    .whereNotEqualTo("id", currentMessageId) // Exclude current message
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(5) // Get last 5 messages
                    .get()
                    .await()
                    
                // Process messages and add to messaging style
                val processedMessages = messages.documents.mapNotNull { doc ->
                    try {
                        val message = doc.toObject(com.example.childsafe.data.model.Message::class.java)
                            ?: return@mapNotNull null
                            
                        // For each message, get sender name
                        val senderDoc = firestore.collection("users").document(message.sender).get().await()
                        val senderName = senderDoc.getString("displayName") ?: message.sender
                        
                        // Create person for this sender
                        val sender = Person.Builder()
                            .setName(senderName)
                            .setIcon(IconCompat.createWithResource(context, R.drawable.avatar_default))
                            .setKey(message.sender)
                            .build()
                            
                        // Create the message
                        NotificationCompat.MessagingStyle.Message(
                            message.text,
                            message.timestamp.toDate().time,
                            sender
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to process message for notification history")
                        null
                    }
                }
                
                // Add messages in chronological order (oldest first)
                processedMessages.reversed().forEach { messageItem ->
                    messagingStyle.addMessage(messageItem)
                }
                
                // Re-issue the notification with the updated style
                // Use our helper method that handles permissions and null checks
                postNotificationWithPermissionCheck(
                    conversationId.hashCode(),
                    messagingStyle.build()
                )
                }
            } catch (e: Exception) {
            Timber.e(e, "Failed to load message history for notifications")
        }
    }

    /**
     * Determine which notification channel to use based on message content and metadata
     * 
     * @param message The message to analyze
     * @param data Additional data from FCM payload
     * @return Channel ID to use for the notification
     */
    private fun determineNotificationChannel(message: Message, data: Map<String, String>? = null): String {
        // Check for SOS/emergency message keywords
        val emergencyKeywords = listOf("sos", "emergency", "help", "urgent", "danger", "medical")
        val text = message.text.lowercase()
        
        // Check if this is flagged as emergency message
        val isEmergency = data?.get("isEmergency")?.toBoolean() == true
        
        // Check if message contains any emergency keywords
        val containsEmergencyKeyword = emergencyKeywords.any { keyword -> text.contains(keyword) }
        
        // Use SOS channel for emergency messages
        if (isEmergency || containsEmergencyKeyword) {
            return SOS_CHANNEL_ID
        }
        
        // Check if message is important
        val isImportant = data?.get("isImportant")?.toBoolean() ?: false
        
        // Check if message is from a trusted contact
        val fromTrustedContact = data?.get("fromTrustedContact")?.toBoolean() ?: false
        
        // Use important channel for important or trusted contact messages
        if (isImportant || fromTrustedContact) {
            return IMPORTANT_CHANNEL_ID
        }
        
        // Default to regular chat channel
        return CHAT_CHANNEL_ID
    }

    /**
     * Helper method to post notifications with proper permission check
     * 
     * @param notificationId The ID of the notification
     * @param notification The notification to show
     */
    private fun postNotificationWithPermissionCheck(notificationId: Int, notification: android.app.Notification?) {
        // Skip if notification is null
        if (notification == null) {
            Timber.w("Cannot post null notification")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, notification)
            } else {
                Timber.w("Cannot post notification: POST_NOTIFICATIONS permission not granted")
            }
        } else {
            // For versions before Android 13, no runtime permission check needed
            notificationManager.notify(notificationId, notification)
        }
    }
}
