/**
 * Message Notification Function
 * 
 * Sends push notifications for new chat messages
 * 
 * This function:
 * 1. Triggers when new messages are created in Firestore
 * 2. Gets user details for the sender and all recipients
 * 3. Retrieves FCM tokens for all recipients
 * 4. Sends properly formatted chat notification to recipients' devices
 */

const admin = require('firebase-admin');
const functions = require('firebase-functions');
const db = admin.firestore();

// Collection references
const userFcmTokensRef = db.collection('userFcmTokens');
const usersRef = db.collection('users');
const conversationsRef = db.collection('conversations');

/**
 * Firebase Cloud Function to send message notifications
 * Triggered when a new message document is created in Firestore
 */
exports.sendChatMessageNotification = functions.firestore
  .document('messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    try {
      const messageData = snapshot.data();
      const messageId = context.params.messageId;
      
      // Skip notifications for system messages if needed
      if (messageData.messageType === 'system') {
        return null;
      }
      
      // Get conversation details to determine recipients
      const conversationRef = conversationsRef.doc(messageData.conversationId);
      const conversation = await conversationRef.get();
      
      if (!conversation.exists) {
        console.error('Conversation not found:', messageData.conversationId);
        return null;
      }
      
      const conversationData = conversation.data();
      const participants = conversationData.participants || [];
      
      // Skip if no participants
      if (!participants.length) {
        console.log('No participants in conversation');
        return null;
      }
      
      // Get sender information
      const senderId = messageData.sender;
      let senderName = 'User';
      let senderAvatarUrl = null;
      
      try {
        const senderDoc = await usersRef.doc(senderId).get();
        if (senderDoc.exists) {
          const senderData = senderDoc.data();
          senderName = senderData.displayName || senderData.firstName || senderData.username || 'User';
          senderAvatarUrl = senderData.profileImageUrl || senderData.avatarUrl || null;
        }
      } catch (error) {
        console.error('Error getting sender data:', error);
      }
      
      // Don't send notifications to the sender
      const recipients = participants.filter(userId => userId !== senderId);
      
      // Get all FCM tokens for recipients
      const tokens = [];
      const recipientData = {};
      
      for (const userId of recipients) {
        try {
          // Get user data for personalization
          const userDoc = await usersRef.doc(userId).get();
          if (userDoc.exists) {
            const userData = userDoc.data();
            recipientData[userId] = {
              displayName: userData.displayName || userData.firstName || userData.username || 'User'
            };
          }
          
          // Query for all tokens associated with this user
          const userTokensSnapshot = await userFcmTokensRef
            .where('userId', '==', userId)
            .get();
          
          userTokensSnapshot.forEach(doc => {
            const tokenData = doc.data();
            if (tokenData.token) {
              tokens.push({
                token: tokenData.token,
                userId: userId
              });
            }
          });
        } catch (error) {
          console.error(`Error getting data for user ${userId}:`, error);
        }
      }
      
      // If no tokens found, exit
      if (tokens.length === 0) {
        console.log('No FCM tokens found for recipients');
        return null;
      }
      
      // Prepare notification payload
      const timestamp = messageData.timestamp ? messageData.timestamp.toMillis() : Date.now();
      
      // Create appropriate message content based on type
      let messageText = messageData.text || '';
      if (messageData.messageType === 'image') {
        messageText = '📷 Image';
      } else if (messageData.messageType === 'audio') {
        messageText = '🎤 Voice message';
      } else if (messageData.messageType === 'location') {
        messageText = '📍 Location';
      }
      
      // Group tokens by user for personalized notifications
      const userTokenGroups = {};
      tokens.forEach(item => {
        if (!userTokenGroups[item.userId]) {
          userTokenGroups[item.userId] = [];
        }
        userTokenGroups[item.userId].push(item.token);
      });
      
      // Send personalized notifications to each user
      const notificationPromises = [];
      
      for (const userId in userTokenGroups) {
        const userTokens = userTokenGroups[userId];
        const recipientName = recipientData[userId]?.displayName || 'User';
        
        // Create base notification data
        const notificationData = {
          type: 'chat_message',
          conversationId: messageData.conversationId,
          messageId: messageId,
          senderId: senderId,
          senderName: senderName,
          messageText: messageText,
          messageType: messageData.messageType || 'text',
          timestamp: timestamp.toString(),
          isGroup: conversationData.isGroup ? 'true' : 'false',
          groupName: conversationData.groupName || ''
        };
        
        // Add media URL if present
        if (messageData.mediaUrl) {
          notificationData.mediaUrl = messageData.mediaUrl;
        }
        
        // Add location data if present
        if (messageData.location) {
          notificationData.locationLatitude = messageData.location.latitude?.toString() || '0';
          notificationData.locationLongitude = messageData.location.longitude?.toString() || '0';
          notificationData.locationName = messageData.location.locationName || '';
        }
        
        // Add sender avatar if available
        if (senderAvatarUrl) {
          notificationData.senderAvatarUrl = senderAvatarUrl;
        }
        
        // Create personalized message for this recipient
        const message = {
          data: notificationData,
          tokens: userTokens
        };
        
        // Send notification and collect promise
        notificationPromises.push(
          admin.messaging().sendMulticast(message)
            .then(response => {
              console.log(`Sent notification to user ${userId}: ${response.successCount} successful, ${response.failureCount} failed`);
              
              // Handle failed tokens
              if (response.failureCount > 0) {
                const failedTokens = [];
                response.responses.forEach((resp, idx) => {
                  if (!resp.success) {
                    failedTokens.push(userTokens[idx]);
                  }
                });
                
                // Clean up invalid tokens
                return cleanupInvalidTokens(failedTokens);
              }
              
              return null;
            })
            .catch(error => {
              console.error(`Error sending notification to user ${userId}:`, error);
              return null;
            })
        );
      }
      
      // Wait for all notifications to be sent
      await Promise.all(notificationPromises);
      
      return null;
    } catch (error) {
      console.error('Error sending chat notification:', error);
      return null;
    }
  });

/**
 * Clean up invalid FCM tokens
 * @param {string[]} tokens - Array of invalid tokens
 */
async function cleanupInvalidTokens(tokens) {
  if (!tokens || tokens.length === 0) return;
  
  try {
    // Find and delete invalid tokens
    const batch = db.batch();
    let deletionCount = 0;
    
    for (const token of tokens) {
      const invalidTokenDocs = await userFcmTokensRef
        .where('token', '==', token)
        .get();
        
      invalidTokenDocs.forEach(doc => {
        batch.delete(doc.ref);
        deletionCount++;
      });
    }
    
    if (deletionCount > 0) {
      await batch.commit();
      console.log(`Cleaned up ${deletionCount} invalid FCM tokens`);
    }
  } catch (error) {
    console.error('Error cleaning up invalid tokens:', error);
  }
}
