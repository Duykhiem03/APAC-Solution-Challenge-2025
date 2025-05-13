package com.example.childsafe.di

import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing BuildConfigStrategy from contexts that don't have 
 * direct dependency injection, like Composables
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BuildConfigEntryPoint {
    fun buildConfigStrategy(): BuildConfigStrategy
}
