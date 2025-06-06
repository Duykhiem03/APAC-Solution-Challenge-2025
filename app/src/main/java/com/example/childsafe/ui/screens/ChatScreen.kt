package com.example.childsafe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.childsafe.ui.components.OnlineStatusIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageType
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.theme.ChildSafeTheme
import com.example.childsafe.ui.viewmodel.MessageViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.childsafe.data.model.MessageStatus
import com.example.childsafe.BuildConfig
import com.example.childsafe.R
import com.example.childsafe.ui.theme.AppDimensions
import com.example.childsafe.ui.viewmodel.ChatViewModel
import com.example.childsafe.utils.BuildConfigHelper
import timber.log.Timber

/**
 * Screen for displaying and interacting with an individual chat conversation
 * 
 * @param conversationId ID of the conversation to display
 * @param onBackClick Callback for when the back button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    messageViewModel: MessageViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    // Load conversation messages
    LaunchedEffect(conversationId) {
        messageViewModel.setConversation(conversationId)
    }

    // Observe UI state from ViewModel
    val uiState by messageViewModel.uiState.collectAsState()
    val messages = uiState.messages
    val conversation = uiState.conversation
    val isLoading = uiState.isLoading
    val currentInput = uiState.currentInput
    val errorMessage = uiState.errorMessage
    val isSendingMessage = uiState.isSendingMessage
    val isOtherUserTyping = uiState.isOtherUserTyping
    val isOtherUserOnline = uiState.isOtherUserOnline
    val isLoadingOlderMessages = uiState.isLoadingOlderMessages
    val isNetworkAvailable = uiState.isNetworkAvailable
    val failedMessages = uiState.failedMessages
    
    // Debug dialog state
    var showMessageStateDialog by remember { mutableStateOf(false) }
    
    // Get buildConfig for the entire component
    val context = LocalContext.current
    val buildConfig = remember { BuildConfigHelper.getBuildConfigStrategy(context) }
    
    // Monitor message updates
    LaunchedEffect(messages.size) {
        Timber.d("ChatScreen: Messages size changed to ${messages.size}")
    }
    
    // Start observing user status (typing/online)
    LaunchedEffect(conversationId) {
        messageViewModel.startObservingUserStatus()
    }
    
    // Clear error message after showing
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            // Show error as a snackbar
            // After a delay, clear the error
            kotlinx.coroutines.delay(3000)
            messageViewModel.clearError()
        }
    }
    
    // Monitor network connectivity 
    val connectivityManager = LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Update network status and handle cleanup
    DisposableEffect(Unit) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                messageViewModel.updateNetworkStatus(true)
            }
            
            override fun onLost(network: Network) {
                messageViewModel.updateNetworkStatus(false)
            }
        }
        
        // Register the callback
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Cleanup when leaving this composable
        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering network callback")
            }
        }
    }
    
    // Scroll state for message list
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Get the contact name (for one-on-one chats)
    val contactName = remember(conversation) {
        if (conversation?.isGroup == true) {
            conversation.groupName
        } else {
            // In a real app, you would get the contact's name from UserRepository
            // Currently using a placeholder
            "Contact"
        }
    }

    // Track visible messages for read receipts
    val visibleMessages = remember { mutableStateListOf<String>() }
    
    // Mark messages as delivered when screen is opened
    LaunchedEffect(conversationId) {
        messageViewModel.markMessagesDelivered()
    }

    // Mark visible messages as read
    LaunchedEffect(messages) {
        // Process newly visible messages that need to be marked as read
        if (messages.isNotEmpty()) {
            messages.forEach { message ->
                val currentUserId = messageViewModel.getCurrentUserId()
                // Only mark other users' messages as read
                if (message.sender != currentUserId && !visibleMessages.contains(message.id)) {
                    // Add to tracking list
                    visibleMessages.add(message.id)
                    // Mark as read
                    messageViewModel.markMessageAsRead(message.id)
                }
            }
        }
    }

    // Update the ViewModel's network status when connectivity changes
    LaunchedEffect(isNetworkAvailable) {
        messageViewModel.updateNetworkStatus(isNetworkAvailable)
    }

    // Start real-time updates for this conversation
    LaunchedEffect(conversationId) {
        // Initialize real-time message updates
        messageViewModel.startRealtimeUpdates(conversationId)
    }
    
    // Clean up when leaving this conversation
    DisposableEffect(conversationId) {
        onDispose {
            messageViewModel.stopRealtimeUpdates()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Network status bar that shows connectivity state and pending messages
            if (!isNetworkAvailable || uiState.failedMessages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (!isNetworkAvailable) Color.Red.copy(alpha = 0.8f)
                            else Color.Blue.copy(alpha = 0.8f)
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isNetworkAvailable) 
                            "No internet connection" 
                        else 
                            "${uiState.failedMessages.size} message(s) pending",
                        color = Color.White
                    )
                }
            }
            
            Scaffold(
        topBar = {
            Column {
                
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Contact avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.AvatarRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contactName.first().toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Contact info
                            Column {
                                Text(
                                    text = contactName,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Online status with animated indicator
                                OnlineStatusIndicator(
                                    isOnline = uiState.isOtherUserOnline,
                                    lastSeen = if (!uiState.isOtherUserOnline && uiState.lastSeenTimestamp != null) {
                                        com.example.childsafe.utils.DateTimeFormatter.formatLastSeen(
                                            uiState.lastSeenTimestamp as? com.google.firebase.Timestamp
                                        )
                                    } else null
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Call button
                        IconButton(onClick = { /* Handle call */ }) {
                            Icon(Icons.Default.Call, contentDescription = "Call")
                        }
                        // Video call button
                        IconButton(onClick = { /* Handle video call */ }) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                        }
                        
                        // Add notification test menu (only in debug builds)
                        if (buildConfig.isDebug) {
                            var showMenu by remember { mutableStateOf(false) }
                            
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Test Menu")
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    // Inject our test helper
                                    val testHelper = androidx.hilt.navigation.compose.hiltViewModel<com.example.childsafe.utils.NotificationTestHelper>()
                                    val conversationObj = uiState.conversation
                                    
                                    DropdownMenuItem(
                                        text = { Text("Test Text Notification") },
                                        onClick = {
                                            conversationObj?.let {
                                                testHelper.testChatMessageNotification(
                                                    conversation = it,
                                                    messageText = "Text notification test message",
                                                    messageType = MessageType.TEXT
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("Test Image Notification") },
                                        onClick = {
                                            conversationObj?.let {
                                                testHelper.testChatMessageNotification(
                                                    conversation = it,
                                                    messageText = "Photo from the park",
                                                    messageType = MessageType.IMAGE
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("Test Audio Notification") },
                                        onClick = {
                                            conversationObj?.let {
                                                testHelper.testChatMessageNotification(
                                                    conversation = it,
                                                    messageText = "Voice message (0:15)",
                                                    messageType = MessageType.AUDIO
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("Test Location Notification") },
                                        onClick = {
                                            conversationObj?.let {
                                                testHelper.testChatMessageNotification(
                                                    conversation = it,
                                                    messageText = "My current location",
                                                    messageType = MessageType.LOCATION
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("Test FCM Payload") },
                                        onClick = {
                                            conversationObj?.let {
                                                testHelper.testFcmPayload(
                                                    conversation = it,
                                                    messageText = "Test FCM message payload"
                                                )
                                            }
                                            showMenu = false
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("Test All Message Types") },
                                        onClick = {
                                            conversationObj?.let {
                                                testHelper.testAllMessageTypes(it)
                                            }
                                            showMenu = false
                                        }
                                    )
                                    
                                    // Add a divider to separate notification tests from other test functions
                                    Divider()
                                    
                                    // Add option to create test conversation
                                    // Get test data helper outside of the onClick lambda
                                    val dataHelper = androidx.hilt.navigation.compose.hiltViewModel<com.example.childsafe.utils.TestDataHelper>()
                                    val localContext = androidx.compose.ui.platform.LocalContext.current
                                    
                                    DropdownMenuItem(
                                        text = { Text("Create Test Conversation") },
                                        onClick = {
                                            // Create a test conversation using the already injected helper
                                            dataHelper.createTestConversation(context = localContext)
                                            showMenu = false
                                        }
                                    )
                                    
                                    // Add message state tracker for debugging
                                    DropdownMenuItem(
                                        text = { Text("Show Message States") },
                                        onClick = {
                                            showMessageStateDialog = true
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            ChatInputBar(
                value = currentInput,
                onValueChange = { messageViewModel.updateInput(it) },
                onSendClick = {
                    if (currentInput.isNotEmpty()) {
                        messageViewModel.sendTextMessage()
                    }
                },
                onMicClick = { /* Handle mic click */ },
                isSending = isSendingMessage
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }.apply {
                    errorMessage?.let {
                        LaunchedEffect(it) {
                            showSnackbar(message = it)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)) // Light gray background for chat
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.Primary
                )
            } else if (messages.isEmpty()) {
                // Empty state
                Text(
                    text = "No messages yet. Start a conversation!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                // Messages list with pull-to-refresh
                PullToRefreshChat(
                    messages = messages,
                    listState = listState,
                    isRefreshing = isLoadingOlderMessages,
                    onRefresh = { messageViewModel.loadOlderMessages() },
                    isOtherUserTyping = isOtherUserTyping,
                    onRetryMessage = { messageViewModel.retryMessage(it) }
                )
            }

            // Show retry button for failed messages
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.failedMessages.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isOtherUserTyping) 96.dp else 64.dp)
            ) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Red.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed messages",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.failedMessages.size} messages failed to send",
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { messageViewModel.forceRetryFailedMessages() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            ),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Text("Retry All")
                        }
                    }
                }
            }
            
            // Show typing indicator above the bottom bar
            androidx.compose.animation.AnimatedVisibility(
                visible = isOtherUserTyping,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
            ) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$contactName is typing...", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        TypingDots()
                    }
                }
            }
            
            // Debug dialog - only in debug mode
            if (showMessageStateDialog && buildConfig.isDebug) {
                AlertDialog(
                    onDismissRequest = { showMessageStateDialog = false },
                    title = { Text("Debug Information") },
                    text = { 
                        Text("Conversation ID: $conversationId\n" +
                             "Message count: ${messages.size}\n" +
                             "Network: ${if (isNetworkAvailable) "Connected" else "Disconnected"}")
                    },
                    confirmButton = {
                        Button(onClick = { showMessageStateDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
        }
    }
}

/**
 * Input bar for typing and sending messages
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    isSending: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimensions.navigationButtonSize)
                    .clip(CircleShape)
                    .background(Color(0xFFBCE9FF))
                    .border(1.dp, AppColors.OnSecondary, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.camera),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))


            // Text field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.SearchBarBorder,
                    unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f)
                ),
                enabled = !isSending
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Show circular progress when sending
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = AppColors.Primary,
                    strokeWidth = 3.dp
                )
            } else {
                // Mic button (when text field is empty) or Send button (when there's text)
                if (value.isEmpty()) {
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFBCE9FF), CircleShape)
                            .border(1.dp, AppColors.OnSecondary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Message",
                            tint = Color.Black
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSendClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFBCE9FF), CircleShape)
                            .border(1.dp, AppColors.OnSecondary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual message item in chat with delivery status and retry option
 */
@Composable
fun MessageItem(
    message: Message,
    onRetryClick: (String) -> Unit = {}
) {
    // Get current user ID from Firebase Auth or use "current-user" for debugging
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentUserId = if (BuildConfig.DEBUG) {
        "current-user"
    } else {
        firebaseUser?.uid ?: "current-user"
    }
    
    // Log the user ID and message sender for debugging
    Timber.d("MessageItem - currentUserId: $currentUserId, messageSender: ${message.sender}, isMatch: ${message.sender == currentUserId}")
    
    val isSentByMe = message.sender == currentUserId
    val messageAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    val messageColor = if (isSentByMe) Color(0xFFBCE9FF) else Color.White
    val textColor = Color.Black
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Format for delivery status
    val isFailedMessage = message.getDeliveryStatusEnum() == MessageStatus.FAILED
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = messageAlignment
    ) {
        // Show retry option for failed messages (only for sender's messages)
        if (isFailedMessage && isSentByMe) {
            Row(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .align(messageAlignment),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Failed to send",
                    fontSize = 12.sp,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onRetryClick(message.id) },
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Retry",
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .border(
                    width = 1.dp,
                    color = if (isFailedMessage) Color.Red.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = messageColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSentByMe) 16.dp else 0.dp,
                        bottomEnd = if (isSentByMe) 0.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            when (message.getMessageTypeEnum()) {
                MessageType.TEXT -> {
                    Text(
                        text = message.text,
                        color = textColor
                    )
                }
                MessageType.IMAGE -> {
                    Text("Image message", color = textColor) // Placeholder
                }
                MessageType.LOCATION -> {
                    Column {
                        Text(
                            text = message.text,
                            color = textColor
                        )
                        if (message.location != null) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    // Open map with this location
                                    val locationIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(
                                            "geo:${message.location.latitude},${message.location.longitude}?q=${message.location.latitude},${message.location.longitude}"
                                        )
                                    )
                                    context.startActivity(locationIntent)
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "View Location",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Location")
                            }
                        }
                    }
                }
                MessageType.AUDIO -> {
                    Text("Audio message", color = textColor) // Placeholder
                }
                MessageType.SOS -> {
                    // Special styling for SOS messages
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "SOS Emergency",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SOS EMERGENCY",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = message.text,
                            color = textColor
                        )
                        
                        if (message.location != null) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    // Open map with this location
                                    val locationIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(
                                            "geo:${message.location.latitude},${message.location.longitude}?q=${message.location.latitude},${message.location.longitude}"
                                        )
                                    )
                                    context.startActivity(locationIntent)
                                },
                                modifier = Modifier.padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "View Emergency Location",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Emergency Location")
                            }
                        }
                    }
                }
            }
        }
        
        // Row for timestamp and delivery status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .padding(top = 2.dp, bottom = 8.dp)
                .padding(horizontal = 4.dp)
                .align(messageAlignment)
        ) {
            // Timestamp
            Text(
                text = dateFormat.format(message.timestamp.toDate()),
                fontSize = 10.sp,
                color = AppColors.TextGray
            )
            
            // If it's my message, show delivery status 
            if (isSentByMe) {
                Spacer(modifier = Modifier.width(4.dp))
                
                // Status icon
                when (message.getDeliveryStatusEnum()) {
                    MessageStatus.SENDING -> {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Sending",
                            tint = AppColors.TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    MessageStatus.SENT -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = AppColors.TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    MessageStatus.DELIVERED -> {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = AppColors.TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    MessageStatus.READ -> {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    MessageStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pull-to-refresh implementation for chat messages
 * Allows loading older messages when pulling down from the top
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullToRefreshChat(
    messages: List<Message>,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isOtherUserTyping: Boolean,
    onRetryMessage: (String) -> Unit = {}
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show refreshing indicator at the top
            if (isRefreshing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppColors.Primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            // Log messages list updates
            if (messages.isNotEmpty()) {
                Timber.d("ChatScreen: Rendering ${messages.size} messages")
            }
            
            // Message items
            items(messages) { message ->
                MessageItem(
                    message = message,
                    onRetryClick = onRetryMessage
                )
            }
            
            // Add space at the bottom for typing indicator
            if (isOtherUserTyping) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        
        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = AppColors.Primary
        )
    }
}

/**
 * Animated typing indicator dots
 * Shows a sequence of animated dots to indicate that someone is typing
 */
@Composable
fun TypingDots() {
    val dotSize = 6.dp
    val animationDuration = 1000 // milliseconds
    
    // Create animated values for each dot with proper coroutine scope
    val coroutineScope = rememberCoroutineScope()
    val dotAnimations = remember {
        List(3) { index ->
            // Stagger the animations
            val delay = index * (animationDuration / 3f)
            val animatable = androidx.compose.animation.core.Animatable(0f)
            
            // Launch the animation in the composable's coroutine scope
            coroutineScope.launch { 
                // Start with delay based on dot position
                kotlinx.coroutines.delay(delay.toLong())
                
                // Continuous animation
                while (true) {
                    // Animate up
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = animationDuration / 2
                        )
                    )
                    // Animate down
                    animatable.animateTo(
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = animationDuration / 2
                        )
                    )
                }
            }
            
            animatable
        }
    }
    
    // Get current animation values
    val dotValues = dotAnimations.map { it.value }
    
    // Draw the dots
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(dotSize * 2)
    ) {
        dotValues.forEach { animationValue ->
            // Calculate the vertical offset based on animation value
            val yOffset = -(animationValue * 4).dp
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = yOffset)
                    .background(
                        color = AppColors.Primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Alternative implementation of typing dots using InfiniteTransition
 * This version is better for previews as it uses the animation system that works in previews
 */
@Composable
fun AnimatedTypingDots() {
    val dotSize = 6.dp
    
    // Create an infinite transition that will animate continuously in preview
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing-dots")
    
    // Create animations for each dot with different phases
    val animations = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.keyframes {
                    durationMillis = 1200
                    0f at 0 + (index * 100)
                    1f at 400 + (index * 100)
                    0f at 800 + (index * 100)
                },
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "dot-$index"
        )
    }
    
    // Draw the dots
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(dotSize * 2)
    ) {
        animations.forEach { animation ->
            // Calculate the vertical offset based on animation value
            val yOffset = -(animation.value * 6).dp
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = yOffset)
                    .background(
                        color = AppColors.Primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(name = "Animated Typing Dots Preview", showBackground = true)
@Composable
fun AnimatedTypingDotsPreview() {
    ChildSafeTheme {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .width(100.dp)
                .height(60.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedTypingDots()
            }
        }
    }
}

/**
 * Preview for ChatScreen with sample data
 */
@Preview(name = "ChatScreen Preview", showBackground = true, showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    // Create sample data for preview
    val sampleConversation = Conversation(
        id = "preview_id", 
        participants = listOf("user1", "user2"),
        isGroup = false,
        groupName = ""
    )
    
    val sampleMessages = listOf(
        Message(
            id = "msg1",
            conversationId = "preview_id",
            sender = "user2",
            text = "Hey, how's it going?",
            timestamp = com.google.firebase.Timestamp.now(),
            messageType = MessageType.TEXT.toString()
        ),
        Message(
            id = "msg2",
            conversationId = "preview_id",
            sender = "currentUserId",
            text = "I'm good, thanks! Just checking on you.",
            timestamp = com.google.firebase.Timestamp.now(),
            messageType = MessageType.TEXT.toString()
        ),
        Message(
            id = "msg3",
            conversationId = "preview_id",
            sender = "user2",
            text = "I'm at school now. Will be home in an hour.",
            timestamp = com.google.firebase.Timestamp.now(),
            messageType = MessageType.TEXT.toString()
        ),
        Message(
            id = "msg4",
            conversationId = "preview_id",
            sender = "currentUserId",
            text = "Ok, let me know when you're home.",
            timestamp = com.google.firebase.Timestamp.now(),
            messageType = MessageType.TEXT.toString()
        )
    )
    
    // Create a preview-friendly version of ChatScreen
    ChildSafeTheme {
        PreviewChatScreen(
            conversation = sampleConversation,
            messages = sampleMessages
        )
    }
}

/**
 * Preview for empty ChatScreen
 */
@Preview(name = "Empty ChatScreen Preview", showBackground = true, showSystemUi = true)
@Composable
fun EmptyChatScreenPreview() {
    val sampleConversation = Conversation(
        id = "preview_id", 
        participants = listOf("user1", "user2"),
        isGroup = false,
        groupName = ""
    )
    
    ChildSafeTheme {
        PreviewChatScreen(
            conversation = sampleConversation,
            messages = emptyList()
        )
    }
}

/**
 * Preview for ChatScreen with loading state
 */
@Preview(name = "Loading ChatScreen Preview", showBackground = true, showSystemUi = true)
@Composable
fun LoadingChatScreenPreview() {
    ChildSafeTheme {
        PreviewChatScreen(
            isLoading = true
        )
    }
}

/**
 * Helper composable for previewing ChatScreen without needing the ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewChatScreen(
    conversation: Conversation? = null,
    messages: List<Message> = emptyList(),
    isLoading: Boolean = false,
    isSendingMessage: Boolean = false
) {
    // Create states for the preview
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Get the contact name
    val contactName = if (conversation?.isGroup == true) {
        conversation.groupName
    } else {
        "Mom" // Preview name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Contact avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColors.AvatarRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contactName.first().toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Contact info
                        Column {
                            Text(
                                text = contactName,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Online",
                                fontSize = 12.sp,
                                color = AppColors.GpsActive
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Call button
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                    // Video call button
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                value = currentInput,
                onValueChange = { currentInput = it },
                onSendClick = { /* Preview only */ },
                onMicClick = { /* Preview only */ },
                isSending = isSendingMessage
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.Primary
                )
            } else if (messages.isEmpty()) {
                // Empty state
                Text(
                    text = "No messages yet. Start a conversation!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                // Messages list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        // For preview purposes, use a consistent method to show message alignment
                        PreviewMessageItem(message = message)
                    }
                }
            }
        }
    }
}

/**
 * Message item specifically for preview (doesn't rely on FirebaseAuth)
 */
@Composable
private fun PreviewMessageItem(message: Message) {
    val isSentByMe = message.sender == "currentUserId" // For preview
    val messageAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    val messageColor = if (isSentByMe) Color(0xFFBCE9FF) else Color.White
    val textColor = Color.Black
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = messageAlignment
    ) {
        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .border(1.dp, Color.Black, RoundedCornerShape(16.dp))
                .background(
                    color = messageColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSentByMe) 16.dp else 0.dp,
                        bottomEnd = if (isSentByMe) 0.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            when (message.getMessageTypeEnum()) {
                MessageType.TEXT -> {
                    Text(
                        text = message.text,
                        color = textColor
                    )
                }
                MessageType.IMAGE -> {
                    Text("Image message", color = textColor)
                }
                MessageType.LOCATION -> {
                    Text("Location message", color = textColor)
                }
                MessageType.AUDIO -> {
                    Text("Audio message", color = textColor)
                }

                MessageType.SOS -> TODO()
            }
        }
        
        // Timestamp
        Text(
            text = dateFormat.format(message.timestamp.toDate()),
            fontSize = 12.sp,
            color = AppColors.TextGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }
}

@Preview(name = "Typing Animation Preview", showBackground = true)
@Composable
fun TypingDotsPreview() {
    ChildSafeTheme {
        // This CompositionLocalProvider enables animations in previews
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.animation.core.Animatable(0f)
            Text(text = "Typing Indicator", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            TypingDots()
        }
    }
}

@Preview(name = "Chat Typing Indicator Preview", showBackground = true)
@Composable
fun ChatWithTypingPreview() {
    ChildSafeTheme {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Mom is typing...", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                TypingDots()
            }
        }
    }
}

/**
 * Network status bar that shows offline/online status
 */
@Composable
fun NetworkStatusBar(
    isNetworkAvailable: Boolean,
    onRetryClick: () -> Unit
) {
    AnimatedVisibility(
        visible = !isNetworkAvailable,
        enter = slideInVertically() + expandVertically(),
        exit = slideOutVertically() + shrinkVertically()
    ) {
        Surface(
            color = if (isNetworkAvailable) AppColors.GpsActive else Color.Red.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalWifiOff,
                        contentDescription = "Network Status",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You're offline. Messages will be sent when you're back online.",
                        color = Color.White,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
