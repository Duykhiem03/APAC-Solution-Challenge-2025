package com.example.childsafe.utils.buildconfig

import com.example.childsafe.BuildConfig
import javax.inject.Inject

/**
 * Release implementation of BuildConfigStrategy
 * Contains all production-specific behavior
 */
class ReleaseBuildConfigStrategy @Inject constructor() : BuildConfigStrategy {
    
    override val isDebug: Boolean = false
    
    override val versionName: String = BuildConfig.VERSION_NAME
    
    override val versionCode: Int = BuildConfig.VERSION_CODE
    
    override fun logDebug(tag: String, message: String) {
        // No-op in release mode - we don't log debug messages
    }
    
    override fun createTestData(): Boolean {
        // Test data creation not supported in release mode
        return false
    }
    
    override fun getTestResource(resourceName: String): Any? {
        // No test resources in release mode
        return null
    }
    
    override fun showTestUiFeatures(): Boolean {
        // Never show test UI features in release mode
        return false
    }
}
