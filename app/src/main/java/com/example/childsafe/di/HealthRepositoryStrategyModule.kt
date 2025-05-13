package com.example.childsafe.di

import com.example.childsafe.domain.repository.strategy.HealthRepositoryStrategy
import com.example.childsafe.domain.repository.strategy.DebugHealthRepositoryStrategy
import com.example.childsafe.domain.repository.strategy.ProductionHealthRepositoryStrategy
import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing the appropriate HealthRepositoryStrategy based on build type
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthRepositoryStrategyModule {
    
    /**
     * Provides the appropriate HealthRepositoryStrategy based on the current build type
     */
    @Provides
    @Singleton
    fun provideHealthRepositoryStrategy(
        debugStrategy: DebugHealthRepositoryStrategy,
        productionStrategy: ProductionHealthRepositoryStrategy,
        buildConfig: BuildConfigStrategy
    ): HealthRepositoryStrategy {
        return if (buildConfig.isDebug) {
            debugStrategy
        } else {
            productionStrategy
        }
    }
}
