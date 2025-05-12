package com.example.childsafe.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Configuration for WorkManager with Hilt integration
 * Note: The actual initialization is done in ChildSafeApp through Configuration.Provider
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
