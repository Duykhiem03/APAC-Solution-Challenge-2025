package com.example.childsafe.utils.buildconfig

/**
 * Strategy interface for build configuration settings.
 * Allows for better testability and separation of debug/release concerns.
 */
interface BuildConfigStrategy {
    /**
     * Whether the app is running in debug mode
     */
    val isDebug: Boolean
    
    /**
     * Get application version name
     */
    val versionName: String
    
    /**
     * Get application version code
     */
    val versionCode: Int
    
    /**
     * Log debug information
     */
    fun logDebug(tag: String, message: String)
    
    /**
     * Create sample/test data for debug purposes
     * @return true if operation is supported and successful
     */
    fun createTestData(): Boolean
    
    /**
     * Get test-specific resources if available
     * @return the resource or null if not available
     */
    fun getTestResource(resourceName: String): Any?
    
    /**
     * Enable test-specific UI features
     * @return true if the feature should be shown
     */
    fun showTestUiFeatures(): Boolean
}
