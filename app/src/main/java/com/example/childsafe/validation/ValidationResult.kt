package com.example.childsafe.validation

/**
 * Represents the result of a validation operation
 */
sealed class ValidationResult {
    /**
     * Validation passed successfully
     */
    object Success : ValidationResult()

    /**
     * Validation failed with specific error message
     */
    data class Error(val message: String, val field: String? = null) : ValidationResult()

    /**
     * Multiple validation errors
     */
    data class Errors(val errors: List<Error>) : ValidationResult() {
        constructor(vararg errors: Error) : this(errors.toList())
    }

    /**
     * Check if validation was successful
     */
    fun isValid(): Boolean = this is Success

    /**
     * Returns all error messages, or empty list if validation passed
     */
    fun getErrorMessages(): List<String> {
        return when (this) {
            is Success -> emptyList()
            is Error -> listOf(message)
            is Errors -> errors.map { it.message }
        }
    }

    /**
     * Get first error message or null if validation passed
     */
    fun getFirstErrorOrNull(): String? {
        return when (this) {
            is Success -> null
            is Error -> message
            is Errors -> errors.firstOrNull()?.message
        }
    }

    companion object {
        /**
         * Create a successful validation result
         */
        fun success() = Success

        /**
         * Create an error validation result
         */
        fun error(message: String, field: String? = null) = Error(message, field)

        /**
         * Create a validation result with multiple errors
         */
        fun errors(errors: List<Error>) = Errors(errors)

        /**
         * Combine multiple validation results
         * Returns success only if all validations passed
         */
        fun combine(vararg results: ValidationResult): ValidationResult {
            val errors = results.flatMap {
                when (it) {
                    is Success -> emptyList()
                    is Error -> listOf(it)
                    is Errors -> it.errors
                }
            }

            return if (errors.isEmpty()) Success else Errors(errors)
        }
    }
}
