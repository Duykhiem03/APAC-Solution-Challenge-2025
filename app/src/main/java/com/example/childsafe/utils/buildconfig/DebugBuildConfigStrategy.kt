package com.example.childsafe.utils.buildconfig

import com.example.childsafe.BuildConfig
import com.example.childsafe.test.SampleChatData
import timber.log.Timber
import javax.inject.Inject

/**
 * Debug implementation of BuildConfigStrategy
 * Contains all debug-specific behavior
 */
class DebugBuildConfigStrategy @Inject constructor() : BuildConfigStrategy {
    
    override val isDebug: Boolean = true
    
    override val versionName: String = BuildConfig.VERSION_NAME
    
    override val versionCode: Int = BuildConfig.VERSION_CODE
    
    override fun logDebug(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }
    
    override fun createTestData(): Boolean {
        // Debug mode allows test data creation
        return true
    }
    
    override fun getTestResource(resourceName: String): Any? {
        // Return appropriate test resources based on name
        return when(resourceName) {
            "sample_conversations" -> com.example.childsafe.test.SampleChatData.testConversations
            "sample_messages" -> com.example.childsafe.test.SampleChatData.testConversationMessages
            "sample_user_chats" -> com.example.childsafe.test.SampleChatData.testUserChats
            else -> null
        }
    }
    
    override fun showTestUiFeatures(): Boolean {
        // Always show test UI features in debug mode
        return true
    }
}
