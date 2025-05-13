package com.example.childsafe.domain.repository.strategy

import com.example.childsafe.domain.repository.HealthRepository
import com.example.childsafe.data.repository.FirebaseHealthRepository
import javax.inject.Inject

/**
 * Production implementation of HealthRepositoryStrategy
 * Provides the real Firebase-backed repository implementation
 */
class ProductionHealthRepositoryStrategy @Inject constructor(
    private val productionRepository: FirebaseHealthRepository
) : HealthRepositoryStrategy {
    override fun provideHealthRepository(): HealthRepository = productionRepository
}
