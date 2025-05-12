package com.example.childsafe.auth

/**
 * Utility class for phone number formatting and validation
 */
object PhoneNumberUtils {
    
    /**
     * Format phone number to be consistent across the app
     * Removes any non-digit characters except + sign
     * @param phoneNumber The phone number to format
     * @return The formatted phone number
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        // Remove any non-digit characters except + sign
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }
    
    /**
     * Ensure phone number has international prefix
     * @param phoneNumber The phone number to format
     * @return The phone number with + prefix if needed
     */
    fun ensureInternationalFormat(phoneNumber: String): String {
        val cleaned = formatPhoneNumber(phoneNumber)
        return if (cleaned.startsWith("+")) cleaned else "+$cleaned"
    }
    
    /**
     * Format phone number for display, adding spaces for readability
     * @param phoneNumber The phone number to format
     * @return The formatted phone number for display
     */
    fun formatForDisplay(phoneNumber: String): String {
        val cleaned = formatPhoneNumber(phoneNumber)
        
        // Skip formatting if too short
        if (cleaned.length < 7) return cleaned
        
        // Simple formatting for international numbers
        if (cleaned.startsWith("+")) {
            val countryCode = cleaned.substring(0, 3) // +84, +1, etc
            val remaining = cleaned.substring(3)
            
            // Insert spaces every 3-4 digits depending on length
            return if (remaining.length <= 7) {
                // Short number: +84 123 4567
                "$countryCode ${remaining.take(3)} ${remaining.drop(3)}"
            } else {
                // Longer number: +84 123 456 789
                val part1 = remaining.take(3)
                val part2 = remaining.drop(3).take(3)
                val part3 = remaining.drop(6)
                "$countryCode $part1 $part2 $part3"
            }
        }
        
        // Non-international number
        return if (cleaned.length <= 7) {
            // Short number: 123 4567
            "${cleaned.take(3)} ${cleaned.drop(3)}"
        } else {
            // Longer number: 123 456 7890
            val part1 = cleaned.take(3)
            val part2 = cleaned.drop(3).take(3)
            val part3 = cleaned.drop(6)
            "$part1 $part2 $part3"
        }
    }
}
