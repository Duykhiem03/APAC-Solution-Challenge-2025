package com.example.childsafe.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.childsafe.data.local.OfflineMessageDao
import com.example.childsafe.data.local.OfflineMessageDatabase
import com.example.childsafe.services.ConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDatabaseModule {

    @Singleton
    @Provides
    fun provideOfflineMessageDatabase(app: Application): OfflineMessageDatabase {
        return Room.databaseBuilder(
            app,
            OfflineMessageDatabase::class.java,
            "offline_messages_db"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideOfflineMessageDao(db: OfflineMessageDatabase): OfflineMessageDao {
        return db.offlineMessageDao()
    }
    
    @Provides
    @Singleton
    fun provideConnectionManager(application: Application): ConnectionManager {
        return ConnectionManager(application)
    }
}
