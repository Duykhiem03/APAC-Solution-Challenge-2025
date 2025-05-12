package com.example.childsafe.di

import com.example.childsafe.concurrency.ConflictResolutionService
import com.example.childsafe.concurrency.DocumentVersioningService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConcurrencyModule {

    @Provides
    @Singleton
    fun provideConflictResolutionService(): ConflictResolutionService {
        return ConflictResolutionService()
    }
    
    @Provides
    @Singleton
    fun provideDocumentVersioningService(
        firestore: FirebaseFirestore,
        conflictResolutionService: ConflictResolutionService
    ): DocumentVersioningService {
        return DocumentVersioningService(firestore, conflictResolutionService)
    }
}
