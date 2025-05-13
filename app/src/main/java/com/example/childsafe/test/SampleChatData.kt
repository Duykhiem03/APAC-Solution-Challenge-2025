package com.example.childsafe.test

import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.LastMessage
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageStatus
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.data.model.UserConversation
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

/**
 * Provides sample chat data for testing and development purposes
 */
object SampleChatData {

    // Generate a timestamp within the last 24 hours
    private fun recentTimestamp(minutesAgo: Int = 0): Timestamp {
        val now = Date()
        val past = Date(now.time - (minutesAgo * 60 * 1000))
        return Timestamp(past)
    }

    // Sample conversations for testing
    val testConversations = listOf(
        // One-on-one conversation
        Conversation(
            id = "test-convo-1",
            participants = listOf("current-user", "test-user-1"),
            createdAt = recentTimestamp(60),
            updatedAt = recentTimestamp(5),
            lastMessage = LastMessage(
                text = "Hello! How are you?",
                sender = "test-user-1", 
                timestamp = recentTimestamp(5),
                read = false
            ),
            isGroup = false,
            isParticipantOnline = true
        ),
        
        // Family group conversation
        Conversation(
            id = "test-convo-2",
            participants = listOf("current-user", "test-user-2", "test-user-3", "test-user-4"),
            createdAt = recentTimestamp(120),
            updatedAt = recentTimestamp(30),
            lastMessage = LastMessage(
                text = "Dinner at 7pm tonight?",
                sender = "test-user-3",
                timestamp = recentTimestamp(30),
                read = true
            ),
            isGroup = true,
            groupName = "Family Group",
            groupAdmin = "current-user",
            onlineParticipants = listOf("test-user-3")
        ),
        
        // Friend conversation
        Conversation(
            id = "test-convo-3",
            participants = listOf("current-user", "test-user-5"),
            createdAt = recentTimestamp(240),
            updatedAt = recentTimestamp(120),
            lastMessage = LastMessage(
                text = "Let me know when you arrive at the mall",
                sender = "current-user",
                timestamp = recentTimestamp(120),
                read = true
            ),
            isGroup = false,
            isParticipantOnline = false
        )
    )

    // Sample messages for the first conversation
    val testConversationMessages = mapOf(
        "test-convo-1" to listOf(
            Message(
                id = "msg1",
                conversationId = "test-convo-1",
                text = "Hi there!",
                sender = "current-user",
                timestamp = recentTimestamp(45),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg2",
                conversationId = "test-convo-1",
                text = "Hello! How are you?",
                sender = "test-user-1",
                timestamp = recentTimestamp(30),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg3",
                conversationId = "test-convo-1",
                text = "I'm doing well, thanks for asking.",
                sender = "current-user",
                timestamp = recentTimestamp(15),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg4",
                conversationId = "test-convo-1",
                text = "Are we still meeting tomorrow?",
                sender = "test-user-1",
                timestamp = recentTimestamp(5),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            )
        ),
        
        "test-convo-2" to listOf(
            Message(
                id = "msg5",
                conversationId = "test-convo-2",
                text = "Hi everyone!",
                sender = "current-user",
                timestamp = recentTimestamp(120),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg6",
                conversationId = "test-convo-2",
                text = "Let's plan for the weekend",
                sender = "test-user-3",
                timestamp = recentTimestamp(100),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg7",
                conversationId = "test-convo-2",
                text = "I'm free on Saturday",
                sender = "test-user-2",
                timestamp = recentTimestamp(80),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg8",
                conversationId = "test-convo-2",
                text = "Sunday works better for me",
                sender = "test-user-4",
                timestamp = recentTimestamp(60),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg9",
                conversationId = "test-convo-2",
                text = "Dinner at 7pm tonight?",
                sender = "test-user-3",
                timestamp = recentTimestamp(30),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            )
        ),
        
        "test-convo-3" to listOf(
            Message(
                id = "msg10",
                conversationId = "test-convo-3",
                text = "Are you at the mall yet?",
                sender = "current-user",
                timestamp = recentTimestamp(180),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg11",
                conversationId = "test-convo-3",
                text = "Almost there, about 10 minutes away",
                sender = "test-user-5",
                timestamp = recentTimestamp(150),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            ),
            Message(
                id = "msg12",
                conversationId = "test-convo-3",
                text = "Let me know when you arrive at the mall",
                sender = "current-user",
                timestamp = recentTimestamp(120),
                messageType = MessageType.TEXT.toString(),
                deliveryStatus = MessageStatus.SENT.toString()
            )
        )
    )

    // Sample user chats data with unread counts
    val testUserChats = UserChats(
        userId = "current-user",
        conversations = listOf(
            UserConversation(conversationId = "test-convo-1", unreadCount = 1),
            UserConversation(conversationId = "test-convo-2", unreadCount = 0),
            UserConversation(conversationId = "test-convo-3", unreadCount = 0)
        )
    )
    
    // Function to generate a new message for test purposes
    fun createNewMessage(
        conversationId: String, 
        text: String, 
        sender: String = "current-user",
        type: MessageType = MessageType.TEXT
    ): Message {
        return Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            text = text,
            sender = sender,
            timestamp = Timestamp.now(),
            messageType = type.toString(),
            deliveryStatus = MessageStatus.SENT.toString()
        )
    }
}
