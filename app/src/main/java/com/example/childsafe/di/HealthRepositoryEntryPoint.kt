package com.example.childsafe.di

import com.example.childsafe.domain.repository.strategy.HealthRepositoryStrategy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HealthRepositoryEntryPoint {
    fun healthRepositoryStrategy(): HealthRepositoryStrategy
}
