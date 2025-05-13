package com.example.childsafe.validation

import com.example.childsafe.data.model.UserChats
import com.example.childsafe.data.model.UserConversation

/**
 * Validator for conversation-related data models
 */
object ConversationValidator {

    /**
     * Validates participants list before creating a conversation
     */
    fun validateParticipants(participantIds: List<String>, isGroup: Boolean): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()
        
        // Check if participants list is not empty
        if (participantIds.isEmpty()) {
            errors.add(ValidationResult.error("Participants list cannot be empty", "participants"))
            return ValidationResult.Errors(errors)
        }
        
        // For direct conversations, exactly 2 participants are required
        if (!isGroup && participantIds.size != 2) {
            errors.add(ValidationResult.error("Direct conversations must have exactly 2 participants", "participants"))
        }
        
        // For group conversations, at least 2 participants are required
        if (isGroup && participantIds.size < 2) {
            errors.add(ValidationResult.error("Group conversations must have at least 2 participants", "participants"))
        }
        
        // Check for duplicate participants
        val uniqueParticipants = participantIds.toSet()
        if (uniqueParticipants.size < participantIds.size) {
            errors.add(ValidationResult.error("Duplicate participants are not allowed", "participants"))
        }
        
        // Validate each participant ID
        participantIds.forEach { participantId ->
            if (participantId.isBlank()) {
                errors.add(ValidationResult.error("Participant ID cannot be empty", "participants"))
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.success()
               else ValidationResult.Errors(errors)
    }
    
    /**
     * Validates group name for group conversations
     */
    fun validateGroupName(groupName: String, isGroup: Boolean): ValidationResult {
        // Only validate group name for group conversations
        if (!isGroup) {
            return ValidationResult.success()
        }
        
        return when {
            groupName.isBlank() -> {
                ValidationResult.error("Group name cannot be empty for group conversations", "groupName")
            }
            groupName.length < 3 -> {
                ValidationResult.error("Group name must be at least 3 characters long", "groupName")
            }
            groupName.length > 50 -> {
                ValidationResult.error("Group name cannot exceed 50 characters", "groupName")
            }
            else -> ValidationResult.success()
        }
    }
    
    /**
     * Validates UserChats data after receiving from Firestore
     */
    fun validateUserChats(userChats: UserChats): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()
        
        // Check required fields
        if (userChats.userId.isBlank()) {
            errors.add(ValidationResult.error("User ID is missing", "userId"))
        }
        
        // Validate each conversation reference
        userChats.conversations.forEachIndexed { index, conversation ->
            val conversationValidation = validateUserConversation(conversation)
            if (!conversationValidation.isValid()) {
                when (conversationValidation) {
                    is ValidationResult.Error -> {
                        errors.add(
                            ValidationResult.error(
                                "${conversationValidation.message} (at index $index)",
                                "conversations[$index].${conversationValidation.field}"
                            )
                        )
                    }
                    is ValidationResult.Errors -> {
                        conversationValidation.errors.forEach { error ->
                            errors.add(
                                ValidationResult.error(
                                    "${error.message} (at index $index)",
                                    "conversations[$index].${error.field}"
                                )
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.success()
               else ValidationResult.Errors(errors)
    }
    
    /**
     * Validates a single UserConversation object
     */
    private fun validateUserConversation(conversation: UserConversation): ValidationResult {
        val errors = mutableListOf<ValidationResult.Error>()
        
        if (conversation.conversationId.isBlank()) {
            errors.add(ValidationResult.error("Conversation ID is missing", "conversationId"))
        }
        
        if (conversation.unreadCount < 0) {
            errors.add(ValidationResult.error("Unread count cannot be negative", "unreadCount"))
        }
        
        return if (errors.isEmpty()) ValidationResult.success()
               else ValidationResult.Errors(errors)
    }
}
