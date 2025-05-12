package com.example.childsafe.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.childsafe.ui.components.FriendItem
import com.example.childsafe.ui.components.FriendRequestItem
import com.example.childsafe.ui.components.OnlineStatusDot
import com.example.childsafe.ui.components.SentRequestItem
import com.example.childsafe.ui.components.UserSearchItem
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import com.example.childsafe.BuildConfig
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.childsafe.data.model.Conversation
import com.example.childsafe.data.model.FriendRequest
import com.example.childsafe.data.model.UserChats
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.ui.theme.AppColors
import com.example.childsafe.ui.viewmodel.FriendsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

/**
 * A draggable panel that displays a list of chat conversations
 * The panel height can be adjusted by the user through dragging
 * 
 * @param conversations List of conversations to display
 * @param userChats UserChats data containing unread message counts
 * @param onConversationSelected Callback when a conversation is selected
 * @param onClose Callback when the panel should be closed
 * @param isLoading Whether the conversations are still loading
 * @param onCreateNewChat Callback when the user wants to create a new chat
 */
@Composable
fun ChatListPanel(
    modifier: Modifier = Modifier,
    conversations: List<Conversation>,
    userChats: UserChats? = null,
    onConversationSelected: (String) -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean = false,
    onCreateNewChat: () -> Unit = {},
    friendsViewModel: FriendsViewModel? = null,
    onStartChatWithFriend: (String) -> Unit = {}
) {
    // Add debug logging to see what's happening
    Timber.d("ChatListPanel: Received ${conversations.size} conversations, isLoading=$isLoading, BuildConfig.DEBUG=${BuildConfig.DEBUG}")
    
    // Debug log each conversation in a clearly visible format
    if (conversations.isEmpty()) {
        Timber.e("ChatListPanel: NO CONVERSATIONS RECEIVED - Empty list!")
    } else {
        Timber.d("ChatListPanel: ======= CONVERSATIONS RECEIVED =======")
        conversations.forEachIndexed { index, conversation ->
            Timber.d("ChatListPanel: Conversation #${index+1}: id=${conversation.id}, lastMsg=${conversation.lastMessage?.text ?: "none"}, isGroup=${conversation.isGroup}")
        }
        Timber.d("ChatListPanel: ======= END OF CONVERSATIONS =======")
    }
    
    // Define the panel height states
    val localDensity = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    
    // Define different height states for the panel
    val minHeight = 120.dp
    val initialHeight = screenHeight * 0.4f // 40% of screen initially
    val maxHeight = screenHeight * 0.8f // 80% of screen maximum
    
    // Keep track of the current height
    var currentHeight by remember { mutableStateOf(initialHeight) }
    
    // Animate height changes for smooth transitions
    val animatedHeight by animateDpAsState(
        targetValue = currentHeight,
        label = "panelHeight"
    )
    
    // Define draggable state
    val draggableState = rememberDraggableState { delta ->
        val newHeight = currentHeight - with(localDensity) { delta.toDp() }
        // Constrain the height between min and max values
        currentHeight = newHeight.coerceIn(minHeight, maxHeight)
    }

    // Tab selection state
    var selectedTab by remember { mutableStateOf(0) }
    
    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Add effect to log when selected tab changes
    LaunchedEffect(selectedTab) {
        Timber.d("ChatListPanel: Tab changed to $selectedTab")
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(10f)
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.PeachBackground, // Using AppColors instead of hardcoded color
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Drag handle at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = AppColors.PeachBackground, // Using AppColors
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = draggableState
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val newHeight = currentHeight - dragAmount.toDp(localDensity)
                                currentHeight = newHeight.coerceIn(minHeight, maxHeight)
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Close button
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close"
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Indicator of panel resizing capability
                        Box {
                            if (currentHeight < maxHeight - 40.dp) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Drag up to expand"
                                )
                            } else if (currentHeight > minHeight + 40.dp) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Drag down to collapse"
                                )
                            }
                        }
                    }
                }
                
                // Tab buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tab for "Trò chuyện" (Chat)
                    Button(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) AppColors.TabActiveBlue else AppColors.TabInactiveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Trò chuyện",
                            color = if (selectedTab == 0) Color.White else Color.Black
                        )
                    }
                    
                    // Tab for "Bạn bè" (Friends)
                    Button(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) AppColors.TabActiveBlue else AppColors.TabInactiveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Bạn bè",
                            color = if (selectedTab == 1) Color.White else Color.Black
                        )
                    }
                    
                    // Tab for "Kết bạn" (Make friends)
                    Button(
                        onClick = { selectedTab = 2 },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 2) AppColors.TabActiveBlue else AppColors.TabInactiveBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Kết bạn",
                            color = if (selectedTab == 2) Color.White else Color.Black
                        )
                    }
                }
                
                // Search bar - only for chat tab
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        placeholder = { Text("Tìm kiếm") },
                        leadingIcon = { 
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        shape = RoundedCornerShape(50),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.SearchBarBorder,
                            unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f)
                        )
                    )
                }
                
                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // Chat tab content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Log the current state for debugging
                            val isDebug = BuildConfig.DEBUG
                            LaunchedEffect(conversations, isLoading) {
                                timber.log.Timber.d("ChatListPanel: Debug=${isDebug}, isLoading=$isLoading, conversations=${conversations.size}")
                                conversations.forEachIndexed { index, convo ->
                                    timber.log.Timber.d("ChatListPanel: Conversation #${index+1}: id=${convo.id}, lastMsg=${convo.lastMessage?.text ?: "none"}")
                                }
                            }
                            
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = AppColors.Primary
                                )
                            } else if (conversations.isEmpty()) {
                                // Show message for empty conversations list
                                Text(
                                    text = "No conversations yet",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                // Log this situation
                                timber.log.Timber.d("ChatListPanel: No conversations to display, BuildConfig.DEBUG=${BuildConfig.DEBUG}")
                            } else {
                                // Log that we're displaying conversations
                                timber.log.Timber.d("ChatListPanel: Displaying ${conversations.size} conversations in LazyColumn")
                                
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {                                // Simply show the conversations directly since we know they're not empty
                                    // Show conversations
                                    items(conversations) { conversation ->
                                        // Log that we're rendering this conversation item
                                        timber.log.Timber.d("ChatListPanel: Rendering conversation item for id=${conversation.id}")
                                        
                                        ConversationItem(
                                            conversation = conversation,
                                            unreadCount = userChats?.unreadCountForConversation(conversation.id) ?: 0,
                                            onClick = { onConversationSelected(conversation.id) }
                                        )
                                    }
                                }
                                
                                // Add floating action button for creating a new chat
                                FloatingActionButton(
                                    onClick = onCreateNewChat,
                                    shape = CircleShape,
                                    containerColor = AppColors.Primary,
                                    contentColor = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add, 
                                        contentDescription = "Create New Chat"
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Friends tab with user search and friends list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            val scope = rememberCoroutineScope()
                            var searchQuery by remember { mutableStateOf("") }
                            var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
                            var isSearching by remember { mutableStateOf(false) }
                            var isPhoneSearch by remember { mutableStateOf(false) }
                            
                            // Search mode selector with improved UI
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Tìm kiếm bằng số điện thoại",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Text(
                                        text = if (isPhoneSearch) 
                                            "Đang tìm theo số điện thoại" 
                                        else 
                                            "Tìm theo tên/email",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Switch(
                                    checked = isPhoneSearch,
                                    onCheckedChange = {
                                        isPhoneSearch = it
                                        // Clear search field and results when switching modes
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AppColors.Primary,
                                        checkedTrackColor = AppColors.Primary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { newQuery ->
                                    searchQuery = newQuery
                                    
                                    // Different minimum length based on search type
                                    val minLength = if (isPhoneSearch) 2 else 3
                                    
                                    // Trigger search if query meets minimum length
                                    if (newQuery.length >= minLength) {
                                        isSearching = true
                                        timber.log.Timber.d("ChatListPanel: Starting search with query: '$newQuery', isPhoneSearch: $isPhoneSearch")
                                        
                                        if (isPhoneSearch) {
                                            // Phone number search
                                            timber.log.Timber.d("ChatListPanel: Initiating phone-specific search for: '$newQuery'")
                                            friendsViewModel?.searchUsersByPhoneFromUi(newQuery) { results ->
                                                timber.log.Timber.d("ChatListPanel: Phone search completed, received ${results.size} results")
                                                searchResults = results
                                                isSearching = false
                                                
                                                // Log top results for debugging
                                                results.take(3).forEachIndexed { index, profile ->
                                                    timber.log.Timber.d("ChatListPanel: Phone search result #${index + 1}: " +
                                                            "ID: ${profile.userId}, Name: '${profile.displayName}', Phone: '${profile.phoneNumber}'")
                                                }
                                            } ?: run {
                                                timber.log.Timber.w("ChatListPanel: FriendsViewModel is null, cannot perform phone search")
                                                searchResults = emptyList()
                                                isSearching = false
                                            }
                                        } else {
                                            // Regular name search
                                            timber.log.Timber.d("ChatListPanel: Initiating regular name search for: '$newQuery'")
                                            friendsViewModel?.searchUsersFromUi(newQuery) { results ->
                                                timber.log.Timber.d("ChatListPanel: Regular search completed, received ${results.size} results")
                                                searchResults = results
                                                isSearching = false
                                                
                                                // Log top results for debugging
                                                results.take(3).forEachIndexed { index, profile ->
                                                    timber.log.Timber.d("ChatListPanel: Regular search result #${index + 1}: " +
                                                            "ID: ${profile.userId}, Name: '${profile.displayName}', Phone: '${profile.phoneNumber}'")
                                                }
                                            } ?: run {
                                                timber.log.Timber.w("ChatListPanel: FriendsViewModel is null, cannot perform regular search")
                                                searchResults = emptyList()
                                                isSearching = false
                                            }
                                        }
                                    } else {
                                        timber.log.Timber.d("ChatListPanel: Query too short (${newQuery.length}), minimum $minLength required. Clearing results.")
                                        searchResults = emptyList()
                                        isSearching = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                placeholder = { 
                                    Text(
                                        if (isPhoneSearch) 
                                            "Tìm kiếm bạn bè bằng số điện thoại" 
                                        else 
                                            "Tìm kiếm bạn bè bằng tên"
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    disabledContainerColor = Color.White,
                                    focusedBorderColor = AppColors.Primary,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            
                            // Search results or friends list
                            if ((isPhoneSearch && searchQuery.length >= 2) || (!isPhoneSearch && searchQuery.length >= 3)) {
                                timber.log.Timber.d("ChatListPanel: Rendering search results UI - Query: '$searchQuery', isPhoneSearch: $isPhoneSearch, resultsCount: ${searchResults.size}")
                                
                                // Searching indicator
                                if (isSearching) {
                                    timber.log.Timber.d("ChatListPanel: Displaying search progress indicator")
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = AppColors.Primary)
                                    }
                                } else {
                                    // Search results
                                    if (searchResults.isEmpty()) {
                                        timber.log.Timber.d("ChatListPanel: Displaying 'No users found' message")
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No users found")
                                        }
                                    } else {
                                        timber.log.Timber.d("ChatListPanel: Displaying ${searchResults.size} search results")
                                        LazyColumn {
                                            items(searchResults) { user ->
                                                UserSearchItem(
                                                    user = user,
                                                    onSendRequest = { userId ->
                                                        timber.log.Timber.d("ChatListPanel: User requested to send friend request to ID: $userId")
                                                        friendsViewModel?.sendFriendRequest(userId)
                                                    },
                                                    onStartChat = { userId ->
                                                        timber.log.Timber.d("ChatListPanel: User requested to start chat with ID: $userId")
                                                        onStartChatWithFriend(userId)
                                                    },
                                                    friendsViewModel = friendsViewModel
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Friends list
                                val friends = friendsViewModel?.uiState?.collectAsState()?.value?.friends ?: emptyList()
                                val isLoading = friendsViewModel?.uiState?.collectAsState()?.value?.isLoadingFriends ?: false
                                val error = friendsViewModel?.uiState?.collectAsState()?.value?.friendsError
                                
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = AppColors.Primary)
                                    }
                                } else if (error != null) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Error: $error")
                                    }
                                } else if (friends.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(60.dp),
                                                tint = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No friends yet")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Search for users above to add friends",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn {
                                        items(friends) { friend ->
                                            FriendItem(
                                                friend = friend,
                                                onStartChat = { onStartChatWithFriend(friend.userId) },
                                                onRemoveFriend = { friendsViewModel?.removeFriend(friend.userId) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Friend requests tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            // Tab navigation for received vs. sent requests
                            var requestTabIndex by remember { mutableStateOf(0) }
                            val requestTabs = listOf("Received", "Sent")
                            
                            TabRow(
                                selectedTabIndex = requestTabIndex,
                                containerColor = Color.Transparent,
                                contentColor = AppColors.Primary,
                                divider = { Divider(thickness = 2.dp, color = Color.LightGray) }
                            ) {
                                requestTabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = requestTabIndex == index,
                                        onClick = { requestTabIndex = index },
                                        text = {
                                            Text(
                                                text = title,
                                                fontWeight = if (requestTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Content based on selected request tab
                            when (requestTabIndex) {
                                0 -> {
                                    // Received requests
                                    val receivedRequests = friendsViewModel?.uiState?.collectAsState()?.value?.receivedRequests ?: emptyList()
                                    val isLoading = friendsViewModel?.uiState?.collectAsState()?.value?.isLoadingReceivedRequests ?: false
                                    val error = friendsViewModel?.uiState?.collectAsState()?.value?.receivedRequestsError
                                    
                                    if (isLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = AppColors.Primary)
                                        }
                                    } else if (error != null) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Error: $error")
                                        }
                                    } else if (receivedRequests.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(60.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No friend requests")
                                            }
                                        }
                                    } else {
                                        LazyColumn {
                                            items(receivedRequests) { request ->
                                                FriendRequestItem(
                                                    request = request,
                                                    onAccept = { friendsViewModel?.acceptFriendRequest(request.requestId) },
                                                    onReject = { friendsViewModel?.rejectFriendRequest(request.requestId) }
                                                )
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // Sent requests
                                    val sentRequests = friendsViewModel?.uiState?.collectAsState()?.value?.sentRequests ?: emptyList()
                                    val isLoading = friendsViewModel?.uiState?.collectAsState()?.value?.isLoadingSentRequests ?: false
                                    val error = friendsViewModel?.uiState?.collectAsState()?.value?.sentRequestsError
                                    
                                    if (isLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = AppColors.Primary)
                                        }
                                    } else if (error != null) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Error: $error")
                                        }
                                    } else if (sentRequests.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(60.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No sent requests")
                                            }
                                        }
                                    } else {
                                        LazyColumn {
                                            items(sentRequests) { request ->
                                                SentRequestItem(
                                                    request = request,
                                                    onCancel = { friendsViewModel?.cancelFriendRequest(request.requestId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Sample contacts display - removed condition that was preventing conversation display
                // Only show sample contacts if explicitly requested or as a fallback when there are no conversations
                // This was preventing conversation display in debug mode
                if (false) { // Temporarily disable this sample data section
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {                                    // Sample data based on the image
                        items(sampleContacts) { contact ->
                            SampleContactItem(
                                name = contact.first,
                                chatNumber = contact.second,
                                onClick = { /* Would navigate to chat but using sample data */ }
                            )
                        }
                    }
                }
                
                // Log status at the end of this branch
                LaunchedEffect(Unit) {
                    timber.log.Timber.d("ChatListPanel: Tab 0 rendering complete. showingPanelContents=$selectedTab, hasConversations=${conversations.isNotEmpty()}")
                }
            }
        }
    }
}

/**
 * Extension function to convert pixel delta to dp
 */
private fun Float.toDp(density: androidx.compose.ui.unit.Density): Dp {
    return with(density) { this@toDp.dp }
}

// Sample contact data to match the image
private val sampleContacts = listOf(
    "Mẹ" to "Chat #01",
    "Bố" to "Chat #02",
    "Chị hai" to "Chat #03",
    "Bạn thân" to "Chat #04"
)

/**
 * Sample contact item to match the design in the image
 */
@Composable
fun SampleContactItem(
    name: String,
    chatNumber: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder with background color matching the image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppColors.AvatarRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Text(
                text = chatNumber,
                color = AppColors.TextGray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Single conversation item in the chat list - updated to match the image style
 */
@Composable
fun ConversationItem(
    conversation: Conversation,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    // Log that we're rendering a conversation item
    LaunchedEffect(conversation.id) {
        Timber.d("ConversationItem: Rendering conversation ${conversation.id}, group=${conversation.isGroup}, name=${conversation.groupName}")
    }
    
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Determine display name based on conversation type
    val displayName = if (conversation.isGroup) {
        conversation.groupName.ifEmpty { "Group Chat" }
    } else {
        // For one-on-one chats, use a meaningful name for testing
        val otherParticipant = conversation.participants.firstOrNull { it != "current-user" } ?: "Unknown"
        "User $otherParticipant".replace("test-user-", "")
    }
    
    // Get first letter for avatar
    val avatarInitial = displayName.firstOrNull()?.toString() ?: "?"
    
    // In debug mode, highlight test conversations
    val displayDebugLabel = BuildConfig.DEBUG && conversation.id.startsWith("test-")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with the style matching the image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    // Use different color for debug conversations
                    if (displayDebugLabel) AppColors.Primary else AppColors.AvatarRed
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarInitial,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                
                // Show online status dot
                if (!conversation.isGroup) {
                    OnlineStatusDot(
                        isOnline = conversation.isParticipantOnline ?: false,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            
            Text(
                text = conversation.lastMessage?.text ?: "No messages yet",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AppColors.TextGray,
                fontSize = 14.sp
            )
        }
        
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AppColors.UnreadBadgeRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Helper extension function to get unread count for a conversation
 */
fun UserChats.unreadCountForConversation(conversationId: String): Int {
    return conversations.find { it.conversationId == conversationId }?.unreadCount ?: 0
}