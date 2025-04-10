package com.example.childsafe.di

import com.example.childsafe.data.api.navigation.GoogleMapsDirectionsService
import com.example.childsafe.data.repository.navigation.RouteRepositoryImpl
import com.example.childsafe.domain.repository.navigation.RouteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger Hilt module that provides navigation-related dependencies.
 * This module is designed to be extensible for future backend integration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds
    @Singleton
    abstract fun bindRouteRepository(
        routeRepositoryImpl: RouteRepositoryImpl
    ): RouteRepository

    companion object {
        @Provides
        @Singleton
        fun provideGoogleMapsDirectionsService(@Named("mapsRetrofit") retrofit: Retrofit): GoogleMapsDirectionsService {
            return retrofit.create(GoogleMapsDirectionsService::class.java)
        }
    }
}