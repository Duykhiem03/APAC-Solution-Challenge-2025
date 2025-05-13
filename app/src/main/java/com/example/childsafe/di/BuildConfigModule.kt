package com.example.childsafe.di

import com.example.childsafe.BuildConfig
import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
import com.example.childsafe.utils.buildconfig.DebugBuildConfigStrategy
import com.example.childsafe.utils.buildconfig.ReleaseBuildConfigStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module that provides build configuration strategy based on current build type
 */
@Module
@InstallIn(SingletonComponent::class)
object BuildConfigModule {
    
    /**
     * Provides the appropriate BuildConfigStrategy implementation
     * based on the current build type
     */
    @Provides
    @Singleton
    fun provideBuildConfigStrategy(
        debugStrategy: DebugBuildConfigStrategy,
        releaseStrategy: ReleaseBuildConfigStrategy
    ): BuildConfigStrategy {
        // Use the actual BuildConfig.DEBUG to determine which implementation to use
        return if (BuildConfig.DEBUG) {
            debugStrategy
        } else {
            releaseStrategy
        }
    }
}
