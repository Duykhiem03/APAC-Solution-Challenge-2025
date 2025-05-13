package com.example.childsafe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childsafe.data.model.UserProfile
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(UserSearchUiState())
    val uiState: StateFlow<UserSearchUiState> = _uiState

    // Search query
    private val _searchQuery = MutableStateFlow("")

    init {
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after typing stops
                .collect { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(isSearching = query.isNotBlank()) }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                val searchResults = userRepository.searchUsers(query)
                
                // Filter out current user from results
                val currentUserId = userRepository.getCurrentUserId()
                val filteredResults = searchResults.filter { it.userId != currentUserId }
                
                _uiState.update {
                    it.copy(
                        searchResults = filteredResults,
                        isLoading = false,
                        errorMessage = if (filteredResults.isEmpty()) "No users found" else null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching for users")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error searching for users: ${e.message}"
                    )
                }
            }
        }
    }

    fun startConversation(userProfile: UserProfile) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCreatingConversation = true, errorMessage = null) }
                
                val currentUserId = userRepository.getCurrentUserId() ?: throw IllegalStateException("User not logged in")
                val conversationId = chatRepository.createConversation(
                    participantIds = listOf(currentUserId, userProfile.userId),
                    isGroup = false,
                    groupName = ""
                )
                
                _uiState.update {
                    it.copy(
                        isCreatingConversation = false,
                        newConversationId = conversationId,
                        navigationEvent = NavigationEvent.NavigateToChat(conversationId)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating conversation")
                _uiState.update {
                    it.copy(
                        isCreatingConversation = false,
                        errorMessage = "Failed to create conversation: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetNavigationEvent() {
        _uiState.update { it.copy(navigationEvent = null) }
    }
}

data class UserSearchUiState(
    val searchResults: List<UserProfile> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isCreatingConversation: Boolean = false,
    val errorMessage: String? = null,
    val newConversationId: String? = null,
    val navigationEvent: NavigationEvent? = null
)

sealed class NavigationEvent {
    data class NavigateToChat(val conversationId: String) : NavigationEvent()
}
