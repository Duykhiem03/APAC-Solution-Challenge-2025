package com.example.childsafe.di

import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.strategy.ChatRepositoryStrategy
import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
import com.example.childsafe.domain.repository.strategy.DebugChatRepositoryStrategy
import com.example.childsafe.domain.repository.strategy.ProductionChatRepositoryStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing the appropriate ChatRepositoryStrategy based on build type
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatRepositoryStrategyModule {
    
    /**
     * Provides the appropriate ChatRepositoryStrategy based on the current build type
     */
    @Provides
    @Singleton
    fun provideChatRepositoryStrategy(
        debugStrategy: DebugChatRepositoryStrategy,
        productionStrategy: ProductionChatRepositoryStrategy,
        buildConfig: BuildConfigStrategy
    ): ChatRepositoryStrategy {
        return if (buildConfig.isDebug) {
            debugStrategy
        } else {
            productionStrategy
        }
    }
    
    /**
     * Provides the ChatRepository using the selected strategy
     */
    @Provides
    @Singleton
    fun provideChatRepository(strategy: ChatRepositoryStrategy): ChatRepository {
        return strategy.provideChatRepository()
    }
}
