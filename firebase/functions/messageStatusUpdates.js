/**
 * Firebase Cloud Functions for Message Delivery Status Management
 * This module handles updating message delivery status across devices
 * 
 * Status workflow:
 * SENDING -> SENT -> DELIVERED -> READ
 */
const functions = require('firebase-functions');
// Import the centralized admin instance
const admin = require('./admin');

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * When a new message is created, set its status to SENT 
 * and notify recipients to update their UI
 */
exports.onMessageCreated = functions.firestore
  .document('messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    const messageData = snapshot.data();
    const messageId = context.params.messageId;
    
    // Check if message has all required fields
    if (!messageData || !messageData.conversationId || !messageData.sender) {
      console.log('Message data incomplete', messageId);
      return null;
    }

    // Update to SENT status if it's not already
    if (messageData.deliveryStatus !== 'SENT') {
      try {
        await snapshot.ref.update({
          deliveryStatus: 'SENT'
        });
        console.log(`Message ${messageId} status updated to SENT`);
      } catch (error) {
        console.error('Error updating message status to SENT:', error);
      }
    }
    
    // Notify all participants except the sender about new message
    const conversationRef = db.collection('conversations').doc(messageData.conversationId);
    
    try {
      const conversationDoc = await conversationRef.get();
      if (!conversationDoc.exists) {
        console.log('Conversation not found:', messageData.conversationId);
        return null;
      }
      
      const conversationData = conversationDoc.data();
      const participants = conversationData.participants || [];
      
      // Get FCM tokens for all participants except sender
      const recipientTokensPromises = [];
      participants.forEach(userId => {
        if (userId !== messageData.sender) {
          const userTokensQuery = db.collection('userFcmTokens')
            .where('userId', '==', userId);
            
          recipientTokensPromises.push(userTokensQuery.get());
        }
      });
      
      const recipientTokensSnapshots = await Promise.all(recipientTokensPromises);
      
      // Extract all tokens
      const tokens = [];
      recipientTokensSnapshots.forEach(snapshot => {
        snapshot.docs.forEach(doc => {
          const token = doc.data().token;
          if (token) tokens.push(token);
        });
      });
      
      if (tokens.length === 0) {
        console.log('No recipient tokens found');
        return null;
      }
      
      // Get sender's display name
      const senderDoc = await db.collection('users').doc(messageData.sender).get();
      const senderName = senderDoc.exists ? senderDoc.data().displayName || 'Unknown' : 'Unknown';
      
      // Send FCM message to all recipients
      const messagePayload = {
        data: {
          type: 'chat_message',
          notificationType: 'new_message',
          conversationId: messageData.conversationId,
          messageId: messageId,
          senderId: messageData.sender,
          senderName: senderName,
          messageText: messageData.text || '',
          messageType: messageData.messageType || 'TEXT',
          timestamp: Date.now().toString()
        }
      };
      
      const response = await messaging.sendMulticast({
        tokens: tokens,
        data: messagePayload.data
      });
      
      console.log(`${response.successCount} messages sent successfully`);
      if (response.failureCount > 0) {
        console.log(`${response.failureCount} messages failed to send`);
      }
      
    } catch (error) {
      console.error('Error sending new message notification:', error);
    }

    return null;
  });

/**
 * When a message is received, mark it as DELIVERED
 * This endpoint will be called by recipients when they receive a message
 */
exports.markMessageDelivered = functions.https.onCall(async (data, context) => {
  // Check authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }
  
  // Extract message ID and user ID
  const { messageId } = data;
  const userId = context.auth.uid;
  
  if (!messageId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'The function must be called with a messageId.'
    );
  }
  
  try {
    // Get the message
    const messageRef = db.collection('messages').doc(messageId);
    const messageDoc = await messageRef.get();
    
    if (!messageDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Message not found.'
      );
    }
    
    const messageData = messageDoc.data();
    
    // Verify this user is a participant in the conversation
    const conversationRef = db.collection('conversations').doc(messageData.conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Conversation not found.'
      );
    }
    
    const conversationData = conversationDoc.data();
    if (!conversationData.participants.includes(userId)) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User is not a participant in this conversation.'
      );
    }
    
    // Only update if the user is not the sender and status is not already higher
    if (messageData.sender !== userId && 
       (messageData.deliveryStatus === 'SENDING' || messageData.deliveryStatus === 'SENT')) {
      
      // Update message status to DELIVERED
      await messageRef.update({
        deliveryStatus: 'DELIVERED'
      });
      
      // Notify the sender about delivery status change
      await notifySenderOfStatusChange(messageData.sender, messageId, 'DELIVERED');
      
      return { success: true };
    }
    
    return { success: false, reason: 'No status update needed' };
    
  } catch (error) {
    console.error('Error marking message as delivered:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Error marking message as delivered.',
      error
    );
  }
});

/**
 * When a message is viewed, mark it as READ
 * This endpoint will be called when a user opens and views a message
 */
exports.markMessageRead = functions.https.onCall(async (data, context) => {
  // Check authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }
  
  // Extract message ID and user ID
  const { messageId } = data;
  const userId = context.auth.uid;
  
  if (!messageId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'The function must be called with a messageId.'
    );
  }
  
  try {
    // Get the message
    const messageRef = db.collection('messages').doc(messageId);
    const messageDoc = await messageRef.get();
    
    if (!messageDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Message not found.'
      );
    }
    
    const messageData = messageDoc.data();
    
    // Verify this user is a participant
    const conversationRef = db.collection('conversations').doc(messageData.conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Conversation not found.'
      );
    }
    
    const conversationData = conversationDoc.data();
    if (!conversationData.participants.includes(userId)) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User is not a participant in this conversation.'
      );
    }
    
    // Don't update if the user is the sender
    if (messageData.sender === userId) {
      return { success: false, reason: 'Sender cannot mark their own message as read' };
    }
    
    // Update message status to READ if not already
    if (messageData.deliveryStatus !== 'READ') {
      // Update message status
      await messageRef.update({
        deliveryStatus: 'READ',
        read: true,
        readBy: admin.firestore.FieldValue.arrayUnion(userId)
      });
      
      // Notify sender about read status
      await notifySenderOfStatusChange(messageData.sender, messageId, 'READ');
      
      return { success: true };
    }
    
    return { success: false, reason: 'Message already marked as read' };
    
  } catch (error) {
    console.error('Error marking message as read:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Error marking message as read.',
      error
    );
  }
});

/**
 * Notify the sender about a change in message status
 */
async function notifySenderOfStatusChange(senderId, messageId, newStatus) {
  try {
    // Get sender's FCM tokens
    const tokensSnapshot = await db.collection('userFcmTokens')
      .where('userId', '==', senderId)
      .get();
    
    const tokens = [];
    tokensSnapshot.forEach(doc => {
      const token = doc.data().token;
      if (token) tokens.push(token);
    });
    
    if (tokens.length === 0) {
      console.log('Sender has no FCM tokens registered');
      return;
    }
    
    // Send status update notification
    const payload = {
      data: {
        type: 'status_update',
        notificationType: 'message_status_change',
        messageId: messageId,
        newStatus: newStatus,
        timestamp: Date.now().toString()
      }
    };
    
    const response = await messaging.sendMulticast({
      tokens: tokens,
      data: payload.data
    });
    
    console.log(`Status update sent to ${response.successCount} devices`);
    if (response.failureCount > 0) {
      console.log(`${response.failureCount} devices failed to receive status update`);
    }
    
  } catch (error) {
    console.error('Error notifying sender of status change:', error);
  }
}

/**
 * When a conversation is marked as read, update all messages within it
 * This will be triggered when a user "catches up" with all messages
 */
exports.markConversationMessagesRead = functions.https.onCall(async (data, context) => {
  // Check authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'The function must be called while authenticated.'
    );
  }
  
  // Extract conversation ID and user ID
  const { conversationId } = data;
  const userId = context.auth.uid;
  
  if (!conversationId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'The function must be called with a conversationId.'
    );
  }
  
  try {
    // Verify user is participant in conversation
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Conversation not found.'
      );
    }
    
    const conversationData = conversationDoc.data();
    if (!conversationData.participants.includes(userId)) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'User is not a participant in this conversation.'
      );
    }
    
    // Get all unread messages not sent by this user
    const unreadMessagesQuery = await db.collection('messages')
      .where('conversationId', '==', conversationId)
      .where('read', '==', false)
      .where('sender', '!=', userId)
      .get();
    
    if (unreadMessagesQuery.empty) {
      console.log('No unread messages to update');
      return { success: true, count: 0 };
    }
    
    // Track senders to notify
    const sendersToNotify = new Set();
    
    // Update each message
    const batch = db.batch();
    unreadMessagesQuery.forEach(doc => {
      const messageData = doc.data();
      sendersToNotify.add(messageData.sender);
      
      batch.update(doc.ref, {
        deliveryStatus: 'READ',
        read: true,
        readBy: admin.firestore.FieldValue.arrayUnion(userId)
      });
    });
    
    await batch.commit();
    
    // Notify each sender about read status
    const promises = [];
    sendersToNotify.forEach(senderId => {
      promises.push(notifySenderOfBulkRead(senderId, conversationId, userId));
    });
    
    await Promise.all(promises);
    
    return { 
      success: true, 
      count: unreadMessagesQuery.size
    };
    
  } catch (error) {
    console.error('Error marking conversation messages as read:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Error updating message statuses.',
      error
    );
  }
});

/**
 * Notify a sender that all their messages in a conversation have been read
 */
async function notifySenderOfBulkRead(senderId, conversationId, readerId) {
  try {
    // Get sender's FCM tokens
    const tokensSnapshot = await db.collection('userFcmTokens')
      .where('userId', '==', senderId)
      .get();
    
    const tokens = [];
    tokensSnapshot.forEach(doc => {
      const token = doc.data().token;
      if (token) tokens.push(token);
    });
    
    if (tokens.length === 0) {
      console.log('Sender has no FCM tokens registered');
      return;
    }
    
    // Get reader's name
    const readerDoc = await db.collection('users').doc(readerId).get();
    const readerName = readerDoc.exists ? readerDoc.data().displayName || 'Someone' : 'Someone';
    
    // Send bulk read notification
    const payload = {
      data: {
        type: 'status_update',
        notificationType: 'conversation_read',
        conversationId: conversationId,
        readerId: readerId,
        readerName: readerName,
        timestamp: Date.now().toString()
      }
    };
    
    const response = await messaging.sendMulticast({
      tokens: tokens,
      data: payload.data
    });
    
    console.log(`Bulk read notification sent to ${response.successCount} devices`);
    if (response.failureCount > 0) {
      console.log(`${response.failureCount} devices failed to receive bulk read notification`);
    }
    
  } catch (error) {
    console.error('Error notifying sender of bulk read:', error);
  }
}
