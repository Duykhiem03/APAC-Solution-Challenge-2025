package com.example.childsafe.di

import android.content.Context
import com.example.childsafe.data.repository.ChatRepositoryImpl
import com.example.childsafe.services.ChatNotificationService
import com.example.childsafe.utils.NotificationTestHelper
import com.example.childsafe.utils.TestDataHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for test utilities
 */
@Module
@InstallIn(SingletonComponent::class)
object TestModule {
    
    /**
     * Provides the NotificationTestHelper for testing notification functionality
     */
    @Provides
    @Singleton
    fun provideNotificationTestHelper(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        chatNotificationService: ChatNotificationService
    ): NotificationTestHelper {
        return NotificationTestHelper(context, firestore, auth, chatNotificationService)
    }
    
    /**
     * Provides the TestDataHelper for creating test data
     */
    @Provides
    @Singleton
    fun provideTestDataHelper(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        chatRepository: ChatRepositoryImpl
    ): TestDataHelper {
        return TestDataHelper(firestore, auth, chatRepository)
    }
}
