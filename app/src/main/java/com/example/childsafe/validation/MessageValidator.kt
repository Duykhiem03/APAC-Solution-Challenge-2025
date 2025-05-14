package com.example.childsafe.validation

import com.example.childsafe.data.model.Message
import com.example.childsafe.data.model.MessageLocation
import com.example.childsafe.data.model.MessageType

/**
 * Validator for Message-related data models
 */
object MessageValidator {
    
    /**
     * Validates a message before sending to Firestore
     */
    fun validateMessage(
        text: String,
        messageType: MessageType,
        mediaUrl: String?,
        location: MessageLocation?
    ): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()
        
        // Validate text
        when (messageType) {
            MessageType.TEXT -> {
                if (text.isBlank()) {
                    errors.add(ValidationResult.error("Text message cannot be empty", "text"))
                } else if (text.length > 2000) {
                    errors.add(ValidationResult.error("Text message is too long (max 2000 characters)", "text"))
                }
            }
            MessageType.IMAGE, MessageType.AUDIO -> {
                if (mediaUrl.isNullOrBlank()) {
                    errors.add(ValidationResult.error("Media URL is required for ${messageType.name} messages", "mediaUrl"))
                } else if (!isValidUrl(mediaUrl)) {
                    errors.add(ValidationResult.error("Invalid media URL format", "mediaUrl"))
                }
            }
            MessageType.LOCATION -> {
                if (location == null) {
                    errors.add(ValidationResult.error("Location is required for LOCATION messages", "location"))
                } else {
                    val locationValidation = validateLocation(location)
                    if (!locationValidation.isValid()) {
                        when (locationValidation) {
                            is ValidationResult.Error -> errors.add(locationValidation)
                            is ValidationResult.Errors -> errors.addAll(locationValidation.errors)
                            else -> {}
                        }
                    }
                }
            }

            MessageType.SOS -> TODO()
        }
        
        return if (errors.isEmpty()) ValidationResult.success() 
               else ValidationResult.Errors(errors)
    }
    
    /**
     * Validates a Message object after receiving from Firestore
     */
    fun validateReceivedMessage(message: Message): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()
        
        // Check required fields
        if (message.id.isBlank()) {
            errors.add(ValidationResult.error("Message ID is missing", "id"))
        }
        
        if (message.conversationId.isBlank()) {
            errors.add(ValidationResult.error("Conversation ID is missing", "conversationId"))
        }
        
        if (message.sender.isBlank()) {
            errors.add(ValidationResult.error("Sender ID is missing", "sender"))
        }
        
        // Validate based on message type
        try {
            val messageType = MessageType.valueOf(message.messageType)
            when (messageType) {
                MessageType.TEXT -> {
                    if (message.text.isBlank()) {
                        errors.add(ValidationResult.error("Text message content is missing", "text"))
                    }
                }
                MessageType.IMAGE, MessageType.AUDIO -> {
                    if (message.mediaUrl.isNullOrBlank()) {
                        errors.add(
                            ValidationResult.error(
                                "Media URL is missing for ${message.messageType} message",
                                "mediaUrl"
                            )
                        )
                    }
                }
                MessageType.LOCATION -> {
                    if (message.location == null) {
                        errors.add(
                            ValidationResult.error(
                                "Location data is missing for LOCATION message",
                                "location"
                            )
                        )
                    } else {
                        val locationValidation = validateLocation(message.location)
                        if (!locationValidation.isValid()) {
                            when (locationValidation) {
                                is ValidationResult.Error -> errors.add(locationValidation)
                                is ValidationResult.Errors -> errors.addAll(locationValidation.errors)
                                else -> {}
                            }
                        }
                    }
                }

                MessageType.SOS -> TODO()
            }
        } catch (e: IllegalArgumentException) {
            errors.add(ValidationResult.error("Invalid message type: ${message.messageType}", "messageType"))
        }
        
        return if (errors.isEmpty()) ValidationResult.success()
               else ValidationResult.Errors(errors)
    }
    
    /**
     * Validates location data
     */
    private fun validateLocation(location: MessageLocation): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()
        
        // Validate latitude (range: -90 to 90)
        if (location.latitude < -90.0 || location.latitude > 90.0) {
            errors.add(ValidationResult.error("Invalid latitude value (must be between -90 and 90)", "latitude"))
        }
        
        // Validate longitude (range: -180 to 180)
        if (location.longitude < -180.0 || location.longitude > 180.0) {
            errors.add(ValidationResult.error("Invalid longitude value (must be between -180 and 180)", "longitude"))
        }
        
        return if (errors.isEmpty()) ValidationResult.success()
               else ValidationResult.Errors(errors)
    }
    
    /**
     * Very basic URL validation
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://") ||
               url.startsWith("gs://") // For Firebase Storage URLs
    }
}
