/**
 * Chat Feature - Firestore Database Schema
 * 
 * This file defines the Firestore collections and document structures 
 * for the chat functionality in ChildSafe app.
 * 
 * Last updated: May 7, 2025
 */

const admin = require('firebase-admin');
const { FieldValue } = admin.firestore;

/**
 * Chat Schema Definitions
 * Use these as templates when creating documents in Firestore
 */
const chatSchema = {
  /**
   * Conversation schema
   * Collection: conversations
   * Description: Stores metadata about conversations between users
   */
  conversation: {
    participants: [], // Array of user IDs in this conversation
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
    lastMessage: null, // Will be populated with a message object
    isGroup: false, // true for group conversations
    groupName: "", // only for group conversations
    groupAdmin: "" // userId of the group admin (if applicable)
  },

  /**
   * Last message schema (embedded in conversation document)
   */
  lastMessage: {
    text: "",
    sender: "",
    timestamp: FieldValue.serverTimestamp(),
    read: false
  },

  /**
   * Message schema
   * Collection: messages
   * Description: Stores individual messages in conversations
   */
  message: {
    conversationId: "", // Reference to parent conversation
    sender: "", // User who sent the message
    text: "", // Message content
    timestamp: FieldValue.serverTimestamp(), // When the message was sent
    read: false, // Whether the message has been read
    readBy: [], // For group chats, which users have read the message
    messageType: "text", // Can be "text", "image", "location", "audio"
    mediaUrl: "", // URL for image/audio messages
    location: null // Optional location object for location sharing
  },

  /**
   * Location schema for location sharing messages
   */
  messageLocation: {
    latitude: 0,
    longitude: 0,
    locationName: "" // Optional readable name
  },

  /**
   * UserChats schema
   * Collection: userChats
   * Description: Quick lookup for a user's conversations
   */
  userChats: {
    conversations: [] // Array of conversation references
  },

  /**
   * User conversation reference schema (embedded in userChats)
   */
  conversationReference: {
    conversationId: "",
    unreadCount: 0,
    lastAccessed: FieldValue.serverTimestamp()
  }
};

/**
 * Enum for message types
 */
const MessageType = {
  TEXT: "text",
  IMAGE: "image",
  LOCATION: "location",
  AUDIO: "audio"
};

/**
 * Collection names
 */
const Collections = {
  CONVERSATIONS: "conversations",
  MESSAGES: "messages",
  USER_CHATS: "userChats"
};

/**
 * Creates a new conversation object
 * @param {string[]} participantIds - Array of user IDs participating in conversation
 * @param {object} options - Additional options
 * @returns {object} Conversation object ready for Firestore
 */
function createConversation(participantIds, options = {}) {
  const { isGroup = false, groupName = '', groupAdmin = '' } = options;
  
  return {
    ...chatSchema.conversation,
    participants: participantIds,
    isGroup,
    groupName: isGroup ? groupName : '',
    groupAdmin: isGroup ? groupAdmin : ''
  };
}

/**
 * Creates a new message object
 * @param {string} conversationId - ID of the conversation 
 * @param {string} senderId - ID of the user sending the message
 * @param {string} text - Message text
 * @param {string} type - Message type (use MessageType enum)
 * @param {object} additionalData - Additional data based on message type
 * @returns {object} Message object ready for Firestore
 */
function createMessage(conversationId, senderId, text, type = MessageType.TEXT, additionalData = {}) {
  const message = {
    ...chatSchema.message,
    conversationId,
    sender: senderId,
    text,
    readBy: [senderId],
    messageType: type
  };

  // Add type-specific fields
  if (type === MessageType.IMAGE || type === MessageType.AUDIO) {
    message.mediaUrl = additionalData.mediaUrl || '';
  } else if (type === MessageType.LOCATION && additionalData.location) {
    message.location = {
      ...chatSchema.messageLocation,
      latitude: additionalData.location.latitude || 0,
      longitude: additionalData.location.longitude || 0,
      locationName: additionalData.location.locationName || ''
    };
  }

  return message;
}

/**
 * Creates a last message object for conversation updates
 * @param {string} text - Message text
 * @param {string} senderId - ID of sender
 * @returns {object} Last message object ready for Firestore
 */
function createLastMessage(text, senderId) {
  return {
    ...chatSchema.lastMessage,
    text,
    sender: senderId
  };
}

/**
 * Creates a conversation reference for userChats collection
 * @param {string} conversationId - ID of the conversation
 * @returns {object} Conversation reference object ready for Firestore
 */
function createConversationReference(conversationId) {
  return {
    ...chatSchema.conversationReference,
    conversationId
  };
}

module.exports = {
  chatSchema,
  MessageType,
  Collections,
  createConversation,
  createMessage,
  createLastMessage,
  createConversationReference
};

// IMPLEMENTATION NOTES:
// 1. Create indexes for:
//    - messages: conversationId + timestamp (for fetching messages in order)
//    - conversations: participants + updatedAt (for listing recent conversations)
//
// 2. Security Rules:
//    - Users should only read/write conversations they're participants in
//    - Messages should only be readable/writable by conversation participants
//
// 3. Ensure to set up Firebase Cloud Functions for:
//    - Updating conversation's lastMessage when new message is added
//    - Incrementing unreadCount in userChats when new message arrives
//    - Sending push notifications for new messages