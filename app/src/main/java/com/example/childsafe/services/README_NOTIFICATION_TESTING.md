# Chat Notification Testing Guide

This guide provides instructions on how to test the chat notification system in the ChildSafe app.

## In-App Testing

The app includes built-in tools for testing notifications while developing. These tools are only available in debug builds.

### Using the Test Menu in Chat Screen

1. Build and run the app in debug mode
2. Navigate to any chat conversation
3. In the top-right corner of the chat screen, tap the three-dot menu icon
4. Select one of the test options:
   - **Test Text Notification**: Simulates receiving a text message
   - **Test Image Notification**: Simulates receiving an image
   - **Test Audio Notification**: Simulates receiving a voice message
   - **Test Location Notification**: Simulates receiving a shared location
   - **Test FCM Payload**: Simulates receiving an FCM message payload
   - **Test All Message Types**: Sends all message types with short delays

The notifications will appear as if they were sent from another participant in the conversation.

## Command-Line Testing

For more control or for testing when the app is in the background, you can use the included shell script.

### Using the test_notification.sh Script

1. First, find your device's FCM token:
   - In LogCat, filter for "FCM token" after starting the app
   - Or check Firebase Console > Cloud Messaging > App Configuration

2. Edit the script to add your Firebase project's Server Key:
   ```bash
   # Open the script
   nano test_notification.sh
   
   # Edit this line
   SERVER_KEY="YOUR_SERVER_KEY" # Get from Firebase Console > Project Settings > Cloud Messaging
   ```

3. Run the script:
   ```bash
   ./test_notification.sh <FCM_TOKEN> [conversation_id] [sender_name]
   ```

   Example:
   ```bash
   ./test_notification.sh d1ApHWLD_IE:APA91bGJ_3QYlR5YbhXzzUV... conversation_123 "John Smith"
   ```

4. Follow the menu prompts to select which type of notification to send

## Firebase Cloud Function Testing

To test the entire notification pipeline including the Firebase Cloud Function:

1. Create a message in Firestore:
   ```javascript
   db.collection('messages').add({
     conversationId: 'CONVERSATION_ID',
     sender: 'OTHER_USER_ID',  // Not the current user
     text: 'Test message from Firestore',
     timestamp: firebase.firestore.FieldValue.serverTimestamp(),
     read: false,
     readBy: ['OTHER_USER_ID'],
     messageType: 'text'
   })
   ```

2. The Cloud Function should detect this new message and send a notification to all participants except the sender

## Debugging Notification Issues

If notifications aren't working:

1. **Check Channels**: Make sure notification channels are properly set up and not disabled
2. **Check Permissions**: Verify the app has notification permissions
3. **Check FCM Token**: Ensure the FCM token is being properly registered
4. **Check Filters**: Verify notification filtering is working (e.g., not showing notifications for messages you sent)
5. **Check Logs**: Look for errors in LogCat with the tag "ChatNotificationService" or "FirebaseMessaging"
6. **Check Firebase Console**: Check the Cloud Messaging section for delivery reports
