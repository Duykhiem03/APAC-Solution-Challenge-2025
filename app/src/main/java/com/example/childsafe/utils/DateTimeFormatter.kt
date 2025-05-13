package com.example.childsafe.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility class for formatting dates and times in a user-friendly way
 */
object DateTimeFormatter {

    /**
     * Format a timestamp as a relative time string (e.g., "just now", "2 minutes ago")
     * 
     * @param timestamp The Firebase timestamp to format
     * @return A user-friendly string representing when this timestamp occurred
     */
    fun formatAsRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Unknown"
        
        val now = System.currentTimeMillis()
        val timestampMillis = timestamp.toDate().time
        val diffMillis = now - timestampMillis
        
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
        
        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
            else -> {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                "on ${dateFormat.format(timestamp.toDate())}"
            }
        }
    }
    
    /**
     * Format timestamp as "Last seen" text
     * 
     * @param timestamp The Firebase timestamp to format
     * @return A user-friendly string for last seen status
     */
    fun formatLastSeen(timestamp: Timestamp?): String {
        if (timestamp == null) return "Offline"
        return "Last seen ${formatAsRelativeTime(timestamp)}"
    }
    
    /**
     * Format a timestamp for message timestamps
     * 
     * @param timestamp The Firebase timestamp to format
     * @return Time in HH:mm format
     */
    fun formatMessageTime(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(timestamp.toDate())
    }
}
