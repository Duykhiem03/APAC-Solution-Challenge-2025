package com.example.childsafe.utils

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.childsafe.R
import com.example.childsafe.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URL

/**
 * Utility class for notification-related operations.
 * Provides helper methods for creating rich notifications, loading avatars, etc.
 */
object NotificationUtils {
    
    /**
     * Create a messaging style notification for conversation
     * 
     * @param context Application context
     * @param builder NotificationCompat.Builder to configure
     * @param currentUserName Current user's name for direct reply
     * @param messages List of messages to show in notification
     * @param senderName Name of the message sender
     * @param senderAvatar Bitmap of sender's avatar (optional)
     */
    fun createMessagingStyleNotification(
        context: Context,
        builder: NotificationCompat.Builder,
        currentUserName: String,
        messages: List<Message>,
        senderName: String,
        senderAvatar: Bitmap? = null
    ): NotificationCompat.Builder {
        // Create Person object for the current user (for direct replies)
        // Ensure the user name is never empty to avoid IllegalArgumentException
        val safeCurrentUserName = currentUserName.takeIf { it.isNotEmpty() } ?: "Me"
        val currentUser = Person.Builder()
            .setName(safeCurrentUserName)
            .setKey("self")
            .build()
            
        // Create Person object for the sender
        // Ensure the sender name is never empty
        val safeSenderName = senderName.takeIf { it.isNotEmpty() } ?: "Unknown"
        val sender = Person.Builder()
            .setName(safeSenderName)
            .setKey(messages.lastOrNull()?.sender ?: "sender")
            
        if (senderAvatar != null) {
            val icon = IconCompat.createWithBitmap(senderAvatar)
            sender.setIcon(icon)
        } else {
            // Use default avatar
            val icon = IconCompat.createWithResource(context, R.drawable.ic_person_placeholder)
            sender.setIcon(icon)
        }
        
        // Create messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(currentUser)
        
        // Add message history (limited to latest 5 messages)
        messages.takeLast(5).forEach { message ->
            val timestamp = message.timestamp.toDate().time
            val personBuilder = if (message.sender == currentUser.key) currentUser else sender.build()
            
            messagingStyle.addMessage(
                when (message.getMessageTypeEnum()) {
                    com.example.childsafe.data.model.MessageType.TEXT -> message.text
                    com.example.childsafe.data.model.MessageType.IMAGE -> "📷 Image"
                    com.example.childsafe.data.model.MessageType.LOCATION -> "📍 Location"
                    com.example.childsafe.data.model.MessageType.AUDIO -> "🎤 Voice message"
                    else -> {
                        Timber.e("Unknown message type: ${message.getMessageTypeEnum()}")
                        "Unknown message"
                    }
                },
                timestamp,
                personBuilder
            )
        }
        
        return builder.setStyle(messagingStyle)
    }
    
    /**
     * Load an image from a URL and convert it to a Bitmap
     * 
     * @param imageUrl URL of the image to load
     * @return Bitmap or null if loading failed
     */
    suspend fun loadAvatarFromUrl(imageUrl: String): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                val connection = URL(imageUrl).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                
                val inputStream = connection.getInputStream()
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load avatar image")
            null
        }
    }
    
    /**
     * Ensures notifications are using the appropriate channel based on importance
     * 
     * @param context Application context
     * @param channelId The channel ID to check
     * @param defaultChannelId Fallback channel ID
     * @return The appropriate channel ID to use
     */
    fun ensureNotificationChannel(
        context: Context,
        channelId: String,
        defaultChannelId: String
    ): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = 
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
            val channel = notificationManager.getNotificationChannel(channelId)
            
            // If channel exists and is not disabled, use it
            if (channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE) {
                return channelId
            }
            
            // Otherwise, use the default channel
            return defaultChannelId
        }
        
        // For pre-Oreo devices, just return the requested channel ID
        return channelId
    }
}
