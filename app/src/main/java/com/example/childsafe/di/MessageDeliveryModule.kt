package com.example.childsafe.di

import android.content.Context
import com.example.childsafe.services.MessageDeliveryService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency module for message delivery services
 */
@Module
@InstallIn(SingletonComponent::class)
object MessageDeliveryModule {

    /**
     * Provides the MessageDeliveryService
     */
    @Singleton
    @Provides
    fun provideMessageDeliveryService(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: FirebaseFunctions
    ): MessageDeliveryService {
        return MessageDeliveryService(context, firestore, auth, functions)
    }
}
