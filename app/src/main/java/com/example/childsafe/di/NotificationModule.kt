package com.example.childsafe.di

import android.content.Context
import com.example.childsafe.services.ChatNotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing notification-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    /**
     * Provides ChatNotificationService instance
     */
    @Provides
    @Singleton
    fun provideChatNotificationService(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): ChatNotificationService {
        return ChatNotificationService(context, firestore, auth)
    }
}
