package com.example.childsafe.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * User's chat summary info for quick access
 */
data class UserChats(
    @DocumentId val userId: String = "",
    val conversations: List<UserConversation> = emptyList()
)

/**
 * Reference to a conversation with unread count
 */
data class UserConversation(
    val conversationId: String = "",
    val unreadCount: Int = 0,
    val lastAccessed: Timestamp = Timestamp.now()
)
