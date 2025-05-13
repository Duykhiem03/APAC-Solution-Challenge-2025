/**
 * Firebase Cloud Functions index file
 * Exports all cloud functions for the ChildSafe app
 */
const functions = require('firebase-functions');

// Import the centralized admin instance (already initialized)
const admin = require('./admin');

// Import message status functions
const messageStatusFunctions = require('./messageStatusUpdates');

// Export the original message status functions
exports.onMessageCreated = messageStatusFunctions.onMessageCreated;
exports.markMessageDelivered = messageStatusFunctions.markMessageDelivered;
exports.markMessageRead = messageStatusFunctions.markMessageRead;
exports.markConversationMessagesRead = messageStatusFunctions.markConversationMessagesRead;

// Import and export notification functions
const chatNotifications = require('./chatNotificationFunction');

exports.sendChatNotification = chatNotifications.sendChatNotification;
exports.sendTypingNotification = chatNotifications.sendTypingNotification;

// Add a basic test function to verify deployment
exports.helloWorld = functions.https.onCall((data, context) => {
  return {
    message: "Hello from Firebase Functions!",
    timestamp: new Date().toISOString(),
    auth: context.auth ? {
      uid: context.auth.uid,
      email: context.auth.token?.email
    } : null
  };
});
