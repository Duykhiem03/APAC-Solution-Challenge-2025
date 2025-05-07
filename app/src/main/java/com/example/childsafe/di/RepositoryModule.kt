package com.example.childsafe.di

import com.example.childsafe.data.repository.ChatRepositoryImpl
import com.example.childsafe.data.repository.SosRepositoryImpl
import com.example.childsafe.data.repository.StorageRepositoryImpl
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.SosRepository
import com.example.childsafe.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
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
}