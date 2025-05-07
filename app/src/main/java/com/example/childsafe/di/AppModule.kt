package com.example.childsafe.di

import android.content.Context
import com.example.childsafe.data.api.LocationApiService
import com.example.childsafe.data.repository.LocationRepositoryImpl
import com.example.childsafe.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Dagger Hilt module that provides application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides Retrofit instance for API calls
     */
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.childsafe-example.com/") // Will be replaced with actual backend URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides LocationApiService instance
     */
    @Provides
    @Singleton
    fun provideLocationApiService(retrofit: Retrofit): LocationApiService {
        return retrofit.create(LocationApiService::class.java)
    }

    /**
     * Provides FusedLocationProviderClient for location services
     */
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Provides LocationRepository implementation
     */
    @Provides
    @Singleton
    fun provideLocationRepository(
        locationApiService: LocationApiService,
        fusedLocationClient: FusedLocationProviderClient,
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepositoryImpl(locationApiService, fusedLocationClient, context)
    }
}