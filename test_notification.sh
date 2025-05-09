#!/bin/bash

# Test script for sending a notification to the ChildSafe app
# This script simulates an FCM notification payload with curl

# Replace these values with your own
PROJECT_ID="childsafe-app"
SERVER_KEY="YOUR_SERVER_KEY" # Get from Firebase Console > Project Settings > Cloud Messaging

# Function to send a notification
send_notification() {
    local token=$1
    local title=$2
    local body=$3
    local conversation_id=$4
    local sender_name=$5
    local message_type=$6
    
    # Current timestamp in milliseconds
    timestamp=$(date +%s%3N)
    
    # Build the FCM payload
    payload=$(cat <<EOF
{
    "to": "$token",
    "data": {
        "type": "chat_message",
        "conversationId": "$conversation_id",
        "messageId": "test_msg_$timestamp",
        "senderId": "test_sender_$timestamp",
        "senderName": "$sender_name",
        "messageText": "$body",
        "messageType": "$message_type",
        "timestamp": "$timestamp",
        "isGroup": "false",
        "groupName": ""
    }
}
EOF
)

    # Send the request to FCM
    curl -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: key=$SERVER_KEY" \
        -d "$payload" \
        https://fcm.googleapis.com/fcm/send
        
    echo -e "\nNotification sent with payload: $payload"
}

# Check if token is provided
if [ $# -lt 1 ]; then
    echo "Usage: $0 <FCM_TOKEN> [conversation_id] [sender_name]"
    echo "Example: $0 d1ApHWLD_IE:APA91bGJ_3QYlR5YbhXzzUV... conversation_123 'John Doe'"
    exit 1
fi

TOKEN=$1
CONVERSATION_ID=${2:-"test_conversation_id"}
SENDER_NAME=${3:-"Test Sender"}

# Menu for selecting test type
echo "Select notification type to send:"
echo "1) Text Message"
echo "2) Image Message"
echo "3) Audio Message"
echo "4) Location Message"
echo "5) Send All Types (with delays)"
read -p "Enter your choice (1-5): " choice

case $choice in
    1)
        send_notification "$TOKEN" "New Message" "This is a test text message" "$CONVERSATION_ID" "$SENDER_NAME" "text"
        ;;
    2)
        send_notification "$TOKEN" "New Image" "📷 Image sent" "$CONVERSATION_ID" "$SENDER_NAME" "image"
        ;;
    3)
        send_notification "$TOKEN" "New Audio" "🎤 Voice message (0:15)" "$CONVERSATION_ID" "$SENDER_NAME" "audio"
        ;;
    4)
        send_notification "$TOKEN" "Location Shared" "📍 Location shared with you" "$CONVERSATION_ID" "$SENDER_NAME" "location"
        ;;
    5)
        send_notification "$TOKEN" "New Message" "This is a test text message" "$CONVERSATION_ID" "$SENDER_NAME" "text"
        sleep 2
        send_notification "$TOKEN" "New Image" "📷 Image sent" "$CONVERSATION_ID" "$SENDER_NAME" "image"
        sleep 2
        send_notification "$TOKEN" "New Audio" "🎤 Voice message" "$CONVERSATION_ID" "$SENDER_NAME" "audio"
        sleep 2
        send_notification "$TOKEN" "Location Shared" "📍 Location shared with you" "$CONVERSATION_ID" "$SENDER_NAME" "location"
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo "Done."
