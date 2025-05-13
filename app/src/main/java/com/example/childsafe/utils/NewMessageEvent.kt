package com.example.childsafe.utils

import com.example.childsafe.data.model.Message

/**
 * Event for new message notifications
 * This event is used to notify components about new messages received from FCM
 */
data class NewMessageEvent(
    val message: Message,
    val conversationId: String,
    val senderId: String,
    val senderName: String
)
