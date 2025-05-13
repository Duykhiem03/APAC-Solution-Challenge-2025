package com.example.childsafe.domain.repository.strategy

import com.example.childsafe.domain.repository.HealthRepository
import com.example.childsafe.data.repository.FakeHealthRepository
import javax.inject.Inject

/**
 * Debug implementation of HealthRepositoryStrategy
 * Provides a debug repository implementation with sample data
 */
class DebugHealthRepositoryStrategy @Inject constructor(
    private val debugRepository: FakeHealthRepository
) : HealthRepositoryStrategy {
    override fun provideHealthRepository(): HealthRepository = debugRepository
}
