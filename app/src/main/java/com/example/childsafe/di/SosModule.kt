package com.example.childsafe.di

import android.content.Context
import com.example.childsafe.data.repository.SosRepositoryImpl
import com.example.childsafe.domain.repository.ChatRepository
import com.example.childsafe.domain.repository.LocationRepository
import com.example.childsafe.domain.repository.SosRepository
import com.example.childsafe.services.SosMonitoringService
import com.example.childsafe.services.SosNotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing SOS-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SosModule {
    
    /**
     * Provides SosNotificationService instance
     */
    @Provides
    @Singleton
    fun provideSosNotificationService(
        @ApplicationContext context: Context
    ): SosNotificationService {
        return SosNotificationService(context)
    }
    
    /**
     * Provides SosMonitoringService instance
     */
    @Provides
    @Singleton
    fun provideSosMonitoringService(
        @ApplicationContext context: Context,
        sosRepository: SosRepository,
        chatRepository: ChatRepository,
        locationRepository: LocationRepository,
        sosNotificationService: SosNotificationService
    ): SosMonitoringService {
        return SosMonitoringService(
            context,
            sosRepository,
            chatRepository,
            locationRepository,
            sosNotificationService
        )
    }
}
