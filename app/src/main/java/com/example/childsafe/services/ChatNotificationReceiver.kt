package com.example.childsafe.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.repository.ChatRepositoryImpl
import com.example.childsafe.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Broadcast receiver for handling chat notification actions
 * such as direct reply and mark as read
 */
@AndroidEntryPoint
class ChatNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPLY = "com.example.childsafe.ACTION_REPLY"
        const val ACTION_MARK_READ = "com.example.childsafe.ACTION_MARK_READ"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
    
    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    @Inject 
    lateinit var functions: com.google.firebase.functions.FirebaseFunctions
    
    @Inject
    lateinit var messageDeliveryService: MessageDeliveryService
    
    @Inject
    lateinit var chatRepository: ChatRepository
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPLY -> handleReply(context, intent)
            ACTION_MARK_READ -> handleMarkRead(context, intent)
        }
    }
    
    private fun handleReply(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra("conversationId") ?: return
        val senderName = intent.getStringExtra("senderName") ?: "User"
        
        // Get the reply text from the RemoteInput
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString() ?: return
        
        // Send the reply message
        scope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                
                chatRepository.sendMessage(
                    conversationId = conversationId,
                    text = replyText,
                    messageType = MessageType.TEXT,
                )
                
                // Cancel the notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(conversationId.hashCode())
                
                Timber.d("Reply sent to $senderName: $replyText")
            } catch (e: Exception) {
                Timber.e(e, "Error sending message reply")
            }
        }
    }
    
    private fun handleMarkRead(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra("conversationId") ?: return
        
        scope.launch {
            try {
                // Mark messages as read in repository
                chatRepository.markConversationAsRead(conversationId)
                
                // Cancel the notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(conversationId.hashCode())
                
                Timber.d("Marked conversation $conversationId as read")
            } catch (e: Exception) {
                Timber.e(e, "Error marking conversation as read")
            }
        }
    }
}
