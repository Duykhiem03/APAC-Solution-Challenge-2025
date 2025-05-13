package com.example.childsafe.domain.repository.strategy

import com.example.childsafe.data.repository.ChatRepositoryImpl
import com.example.childsafe.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Production implementation of ChatRepositoryStrategy
 * Provides the real repository implementation
 */
class ProductionChatRepositoryStrategy @Inject constructor(
    private val chatRepository: ChatRepositoryImpl
) : ChatRepositoryStrategy {
    override fun provideChatRepository(): ChatRepository = chatRepository
}
