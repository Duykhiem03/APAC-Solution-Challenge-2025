package com.example.childsafe.domain.repository.strategy

import com.example.childsafe.domain.repository.ChatRepository

/**
 * Strategy interface for providing a ChatRepository implementation
 * This allows us to easily switch between debug and production implementations
 */
interface ChatRepositoryStrategy {
    /**
     * Get the appropriate ChatRepository implementation based on build type
     * @return ChatRepository implementation
     */
    fun provideChatRepository(): ChatRepository
}
