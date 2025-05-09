/**
 * Chat Data Service - Firebase Firestore
 * 
 * This service provides methods to interact with the Firestore database
 * for the chat functionality in ChildSafe app.
 */

const admin = require('../admin');
const db = admin.firestore();

// Collection references
const conversationsRef = db.collection('conversations');
const messagesRef = db.collection('messages');
const userChatsRef = db.collection('userChats');

/**
 * Chat data service methods for backend operations
 */
const chatDataService = {
  /**
   * Create a new conversation between users
   * @param {string[]} participantIds - Array of user IDs participating in the conversation
   * @param {boolean} isGroup - Whether this is a group conversation
   * @param {string} groupName - Optional group name for group conversations
   * @param {string} groupAdmin - Optional group admin ID
   * @returns {Promise<string>} - ID of the created conversation
   */
  createConversation: async (participantIds, isGroup = false, groupName = '', groupAdmin = '') => {
    // Check if a direct conversation already exists between these users
    if (!isGroup && participantIds.length === 2) {
      const existingConversation = await conversationsRef
        .where('participants', 'array-contains-any', [participantIds[0]])
        .get()
        .then(snapshot => {
          return snapshot.docs.find(doc => {
            const data = doc.data();
            return !data.isGroup && 
              data.participants.length === 2 && 
              data.participants.includes(participantIds[0]) && 
              data.participants.includes(participantIds[1]);
          });
        });
      
      if (existingConversation) {
        return existingConversation.id;
      }
    }
    
    // Create a new conversation
    const conversationData = {
      participants: participantIds,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      lastMessage: null,
      isGroup,
      groupName: isGroup ? groupName : '',
      groupAdmin: isGroup ? groupAdmin : ''
    };
    
    const conversationRef = await conversationsRef.add(conversationData);
    
    // Add this conversation to each participant's userChats document
    const batch = db.batch();
    
    for (const userId of participantIds) {
      const userChatDocRef = userChatsRef.doc(userId);
      
      // Check if the document exists
      const userChatDoc = await userChatDocRef.get();
      
      if (userChatDoc.exists) {
        // Update existing document
        batch.update(userChatDocRef, {
          conversations: admin.firestore.FieldValue.arrayUnion({
            conversationId: conversationRef.id,
            unreadCount: 0,
            lastAccessed: admin.firestore.FieldValue.serverTimestamp()
          })
        });
      } else {
        // Create new document
        batch.set(userChatDocRef, {
          conversations: [{
            conversationId: conversationRef.id,
            unreadCount: 0,
            lastAccessed: admin.firestore.FieldValue.serverTimestamp()
          }]
        });
      }
    }
    
    await batch.commit();
    return conversationRef.id;
  },
  
  /**
   * Send a message in a conversation
   * @param {string} conversationId - ID of the conversation
   * @param {string} senderId - ID of the user sending the message
   * @param {string} text - Message content
   * @param {string} messageType - Type of message (text, image, location, audio)
   * @param {object} additionalData - Additional data based on message type
   * @returns {Promise<string>} - ID of the created message
   */
  sendMessage: async (conversationId, senderId, text, messageType = 'text', additionalData = {}) => {
    // Verify conversation exists and user is participant
    const conversationDoc = await conversationsRef.doc(conversationId).get();
    if (!conversationDoc.exists) {
      throw new Error('Conversation not found');
    }
    
    const conversationData = conversationDoc.data();
    if (!conversationData.participants.includes(senderId)) {
      throw new Error('User is not a participant in this conversation');
    }
    
    // Create message document
    const messageData = {
      conversationId,
      sender: senderId,
      text,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      read: false,
      readBy: [senderId],
      messageType
    };
    
    // Add type-specific fields
    if (messageType === 'image' || messageType === 'audio') {
      messageData.mediaUrl = additionalData.mediaUrl || '';
    } else if (messageType === 'location') {
      messageData.location = additionalData.location || null;
    }
    
    const messageRef = await messagesRef.add(messageData);
    
    // Update the conversation with the last message
    await conversationsRef.doc(conversationId).update({
      lastMessage: {
        text,
        sender: senderId,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        read: false
      },
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // Update unread count for all participants except the sender
    const batch = db.batch();
    
    for (const participantId of conversationData.participants) {
      if (participantId !== senderId) {
        const userChatDocRef = userChatsRef.doc(participantId);
        
        // Get the current user chats
        const userChatDoc = await userChatDocRef.get();
        
        if (userChatDoc.exists) {
          const userData = userChatDoc.data();
          const conversations = userData.conversations || [];
          
          // Find the conversation and update the unread count
          const updatedConversations = conversations.map(conv => {
            if (conv.conversationId === conversationId) {
              return {
                ...conv,
                unreadCount: (conv.unreadCount || 0) + 1
              };
            }
            return conv;
          });
          
          batch.update(userChatDocRef, { conversations: updatedConversations });
        }
      }
    }
    
    await batch.commit();
    return messageRef.id;
  },
  
  /**
   * Mark messages as read
   * @param {string} conversationId - ID of the conversation
   * @param {string} userId - ID of the user marking messages as read
   * @returns {Promise<void>}
   */
  markMessagesAsRead: async (conversationId, userId) => {
    // Get all unread messages in the conversation that weren't sent by this user
    const unreadMessages = await messagesRef
      .where('conversationId', '==', conversationId)
      .where('read', '==', false)
      .where('sender', '!=', userId)
      .get();
    
    if (unreadMessages.empty) {
      return; // No unread messages
    }
    
    // Mark all messages as read using a batch
    const batch = db.batch();
    
    unreadMessages.docs.forEach(doc => {
      batch.update(doc.ref, { 
        read: true,
        readBy: admin.firestore.FieldValue.arrayUnion(userId)
      });
    });
    
    // Update conversation's lastMessage if it was unread
    const conversationRef = conversationsRef.doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (conversationDoc.exists) {
      const conversation = conversationDoc.data();
      
      if (conversation.lastMessage && !conversation.lastMessage.read) {
        batch.update(conversationRef, {
          'lastMessage.read': true
        });
      }
    }
    
    // Reset unread count in userChats
    const userChatDocRef = userChatsRef.doc(userId);
    const userChatDoc = await userChatDocRef.get();
    
    if (userChatDoc.exists) {
      const userData = userChatDoc.data();
      const conversations = userData.conversations || [];
      
      // Find the conversation and reset unread count
      const updatedConversations = conversations.map(conv => {
        if (conv.conversationId === conversationId) {
          return {
            ...conv,
            unreadCount: 0,
            lastAccessed: admin.firestore.FieldValue.serverTimestamp()
          };
        }
        return conv;
      });
      
      batch.update(userChatDocRef, { conversations: updatedConversations });
    }
    
    await batch.commit();
  }
};

module.exports = chatDataService;

// IMPLEMENTATION NOTES:
// 1. This service should be used by your backend API and Firebase Cloud Functions
// 2. For full implementation, add additional methods for:
//    - Fetching conversations for a user
//    - Fetching messages in a conversation (with pagination)
//    - Deleting messages and conversations
//    - Managing group conversations (add/remove participants)
// 3. Remember to set up Firebase Admin SDK properly in your application
// 4. Consider implementing caching for frequently accessed data