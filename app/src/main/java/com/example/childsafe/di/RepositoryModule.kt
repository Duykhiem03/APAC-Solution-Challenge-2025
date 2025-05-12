package com.example.childsafe.di

import com.example.childsafe.data.repository.ChatRepositoryImpl
import com.example.childsafe.data.repository.DebugMessagesRepository
import com.example.childsafe.data.repository.FriendRepositoryImpl
import com.example.childsafe.data.repository.SosRepositoryImpl
import com.example.childsafe.data.repository.StorageRepositoryImpl
import com.example.childsafe.data.repository.UserRepositoryImpl
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.FriendRepository
import com.example.childsafe.domain.repository.SosRepository
import com.example.childsafe.domain.repository.StorageRepository
import com.example.childsafe.domain.repository.UserRepository
import com.example.childsafe.services.FirebaseMessagingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository implementations to their interfaces
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds ChatRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    /**
     * Binds StorageRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository
    
    /**
     * Binds SosRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindSosRepository(
        sosRepositoryImpl: SosRepositoryImpl
    ): SosRepository
    
    /**
     * Binds UserRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    /**
     * Binds FriendRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindFriendRepository(
        friendRepositoryImpl: FriendRepositoryImpl
    ): FriendRepository

    companion object {
        /**
         * Provides FirebaseMessagingManager instance
         */
        @Provides
        @Singleton
        fun provideFirebaseMessagingManager(
            auth: FirebaseAuth,
            firestore: FirebaseFirestore
        ): FirebaseMessagingManager {
            return FirebaseMessagingManager(auth, firestore)
        }
        
        /**
         * Provides DebugMessagesRepository for sharing debug data between ViewModels
         */
        @Provides
        @Singleton
        fun provideDebugMessagesRepository(): DebugMessagesRepository {
            return DebugMessagesRepository()
        }
    }
}