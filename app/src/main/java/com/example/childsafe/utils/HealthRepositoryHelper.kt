package com.example.childsafe.utils

import android.content.Context
import com.example.childsafe.domain.repository.strategy.HealthRepositoryStrategy
import dagger.hilt.android.EntryPointAccessors
import com.example.childsafe.di.HealthRepositoryEntryPoint

object HealthRepositoryHelper {
    fun getHealthRepositoryStrategy(context: Context): HealthRepositoryStrategy {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HealthRepositoryEntryPoint::class.java
        )
        return entryPoint.healthRepositoryStrategy()
    }
}
