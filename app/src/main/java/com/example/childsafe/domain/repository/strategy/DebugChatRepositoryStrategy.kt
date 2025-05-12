package com.example.childsafe.domain.repository.strategy

import com.example.childsafe.data.repository.DebugChatRepository
import com.example.childsafe.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Debug implementation of ChatRepositoryStrategy
 * Provides a debug repository implementation with sample data
 */
class DebugChatRepositoryStrategy @Inject constructor(
    private val debugRepository: DebugChatRepository
) : ChatRepositoryStrategy {
    override fun provideChatRepository(): ChatRepository = debugRepository
}
