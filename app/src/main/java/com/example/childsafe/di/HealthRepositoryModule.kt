package com.example.childsafe.di

import com.example.childsafe.data.repository.FakeHealthRepository
import com.example.childsafe.data.repository.FirebaseHealthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repository implementations for the HealthRepositoryStrategy to use
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthRepositoryModule {
    
    /**
     * Provides the debug/fake repository implementation
     */
    @Provides
    @Singleton
    fun provideFakeHealthRepository(): FakeHealthRepository {
        return FakeHealthRepository()
    }
    
    // Note: FirebaseHealthRepository is provided automatically by Hilt 
    // since it has an @Inject constructor
}
