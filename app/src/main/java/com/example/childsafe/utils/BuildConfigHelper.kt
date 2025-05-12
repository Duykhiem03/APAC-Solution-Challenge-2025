package com.example.childsafe.utils

import android.content.Context
import com.example.childsafe.utils.buildconfig.BuildConfigStrategy
import dagger.hilt.android.EntryPointAccessors
import com.example.childsafe.di.BuildConfigEntryPoint

/**
 * Helper class to access BuildConfigStrategy from Composables
 * This allows components that don't have direct dependency injection
 * to access the BuildConfigStrategy
 */
object BuildConfigHelper {
    /**
     * Get the BuildConfigStrategy instance
     */
    fun getBuildConfigStrategy(context: Context): BuildConfigStrategy {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BuildConfigEntryPoint::class.java
        )
        return entryPoint.buildConfigStrategy()
    }
}
