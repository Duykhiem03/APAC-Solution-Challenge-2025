package com.example.childsafe.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.childsafe.MainActivity
import com.example.childsafe.R
import com.example.childsafe.data.model.SosEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling SOS-related notifications
 */
@Singleton
class SosNotificationService @Inject constructor(
    private val context: Context
) {
    companion object {
        const val SOS_NOTIFICATION_CHANNEL_ID = "sos_notifications"
        const val SOS_ACTIVE_NOTIFICATION_ID = 2001
    }

    init {
        createSosNotificationChannel()
    }

    /**
     * Creates the notification channel for SOS notifications
     */
    private fun createSosNotificationChannel() {
        val name = context.getString(R.string.sos_notification_channel_name)
        val description = context.getString(R.string.sos_notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(SOS_NOTIFICATION_CHANNEL_ID, name, importance).apply {
            this.description = description
            enableVibration(true)
            enableLights(true)
        }

        // Register the channel
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Shows a persistent notification for an active SOS event
     */
    fun showActiveSosNotification(sosEvent: SosEvent) {
        try {
            // Create an intent to open the app when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("sosEventId", sosEvent.id)
                putExtra("isFromSosNotification", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build the notification
            val notification = NotificationCompat.Builder(context, SOS_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
                .setContentTitle(context.getString(R.string.sos_active))
                .setContentText(context.getString(R.string.sos_sharing_location))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // Persistent notification that can't be dismissed
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .build()
            
            // Check for notification permission and show the notification
            val notificationManager = NotificationManagerCompat.from(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13+ (API 33+) we need to explicitly check for notification permission
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(SOS_ACTIVE_NOTIFICATION_ID, notification)
                    Timber.d("Active SOS notification shown for event ${sosEvent.id}")
                } else {
                    Timber.w("Cannot show SOS notification: Notification permission not granted")
                }
            } else {
                // For Android 12 and below, try showing notification and handle potential exception
                try {
                    notificationManager.notify(SOS_ACTIVE_NOTIFICATION_ID, notification)
                    Timber.d("Active SOS notification shown for event ${sosEvent.id}")
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to show SOS notification due to permission denial")
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to show SOS notification")
        }
    }

    /**
     * Dismisses the active SOS notification
     */
    fun dismissActiveSosNotification() {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13+ (API 33+) check permission before canceling
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.cancel(SOS_ACTIVE_NOTIFICATION_ID)
                    Timber.d("Active SOS notification dismissed")
                } else {
                    Timber.w("Cannot dismiss SOS notification: Notification permission not granted")
                }
            } else {
                // For Android 12 and below, try to cancel and catch any security exceptions
                try {
                    notificationManager.cancel(SOS_ACTIVE_NOTIFICATION_ID)
                    Timber.d("Active SOS notification dismissed")
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to dismiss SOS notification due to permission denial")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to dismiss SOS notification: ${e.message}")
        }
    }
}
