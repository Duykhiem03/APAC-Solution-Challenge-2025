package com.example.childsafe.di

import com.example.childsafe.data.repository.MessageUpdateRepository
import com.example.childsafe.concurrency.ConflictResolutionService
import com.example.childsafe.concurrency.DocumentVersioningService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger module that provides data consistency and validation related services
 */
@Module
@InstallIn(SingletonComponent::class)
object DataConsistencyModule {

    @Provides
    @Singleton
    fun provideValidationService(): ValidationService {
        return ValidationService()
    }
    
    @Provides
    @Singleton
    fun provideMessageUpdateRepository(
        documentVersioningService: DocumentVersioningService
    ): MessageUpdateRepository {
        return MessageUpdateRepository(documentVersioningService)
    }
}

/**
 * Service for centralized validation logic
 */
class ValidationService {
    // This class could be expanded to include more validation logic
    // or to centralize validation operations across the app
}
