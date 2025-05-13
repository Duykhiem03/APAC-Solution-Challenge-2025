package com.example.childsafe.domain.repository.strategy

import com.example.childsafe.domain.repository.HealthRepository

/**
 * Strategy interface for providing a HealthRepository implementation
 * This allows us to easily switch between debug and production implementations
 */
interface HealthRepositoryStrategy {
    /**
     * Get the appropriate HealthRepository implementation based on build type
     * @return HealthRepository implementation
     */
    fun provideHealthRepository(): HealthRepository
}
