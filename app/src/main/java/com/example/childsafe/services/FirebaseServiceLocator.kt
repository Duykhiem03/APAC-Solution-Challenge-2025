package com.example.childsafe.services

import javax.inject.Provider

/**
 * Helper class to access services from outside the Hilt dependency injection framework
 * Primarily used by our Firebase Messaging Service which is initialized by the Firebase SDK
 */
object FirebaseServiceLocator {
    
    // Lazy providers to be set by the application
    private lateinit var messageDeliveryServiceProvider: Provider<MessageDeliveryService>
    
    /**
     * Set the provider for MessageDeliveryService
     * Should be called during application initialization
     */
    fun setMessageDeliveryServiceProvider(provider: Provider<MessageDeliveryService>) {
        messageDeliveryServiceProvider = provider
    }
    
    /**
     * Get the MessageDeliveryService instance
     */
    fun getMessageDeliveryService(): MessageDeliveryService {
        if (!::messageDeliveryServiceProvider.isInitialized) {
            throw IllegalStateException("MessageDeliveryService provider not initialized")
        }
        return messageDeliveryServiceProvider.get()
    }
}
