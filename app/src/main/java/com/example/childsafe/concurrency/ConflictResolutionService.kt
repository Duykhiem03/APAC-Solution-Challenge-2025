package com.example.childsafe.concurrency

/**
 * A service for handling conflict resolution in concurrent operations
 */
class ConflictResolutionService {

    /**
     * Resolves conflicts in message updates
     * 
     * @param clientVersion The version of the document when it was read by the client
     * @param serverVersion The current version of the document on the server
     * @param clientData The data the client is trying to write
     * @param serverData The current data on the server
     * @return The resolved data to be written
     */
    fun <T> resolveConflict(
        clientVersion: Long,
        serverVersion: Long,
        clientData: T,
        serverData: T,
        resolver: ConflictResolver<T>
    ): T {
        // If versions match, no conflict
        if (clientVersion == serverVersion) {
            return clientData
        }
        
        // Use the resolver to handle the conflict
        return resolver.resolve(clientData, serverData)
    }
    
    /**
     * Interface for resolving conflicts between client and server data
     */
    interface ConflictResolver<T> {
        /**
         * Resolves a conflict between client and server data
         * 
         * @param clientData The data from the client
         * @param serverData The data from the server
         * @return The resolved data
         */
        fun resolve(clientData: T, serverData: T): T
    }
    
    /**
     * Default implementation that prefers server data (last-write-wins)
     */
    class LastWriteWinsResolver<T> : ConflictResolver<T> {
        override fun resolve(clientData: T, serverData: T): T {
            return serverData
        }
    }
    
    /**
     * Implementation that prefers client data
     */
    class ClientWinsResolver<T> : ConflictResolver<T> {
        override fun resolve(clientData: T, serverData: T): T {
            return clientData
        }
    }

    /**
     * Implementation for merging maps with nested fields
     */
    class MapMergeResolver : ConflictResolver<Map<String, Any?>> {
        override fun resolve(clientData: Map<String, Any?>, serverData: Map<String, Any?>): Map<String, Any?> {
            val result = serverData.toMutableMap()
            
            // Handle special fields based on custom logic
            clientData.forEach { (key, value) ->
                when (key) {
                    // For arrays like "readBy", merge the values
                    "readBy" -> {
                        if (value is List<*> && result[key] is List<*>) {
                            @Suppress("UNCHECKED_CAST")
                            val serverList = result[key] as List<*>
                            @Suppress("UNCHECKED_CAST")
                            val clientList = value as List<*>
                            result[key] = (serverList + clientList).distinct()
                        } else {
                            result[key] = value
                        }
                    }
                    // For delivery status, take the most advanced status
                    "deliveryStatus" -> {
                        if (value is String && result[key] is String) {
                            val serverStatus = result[key] as String
                            val clientStatus = value
                            
                            // Define status progression
                            val statusPriority = mapOf(
                                "SENDING" to 0,
                                "SENT" to 1,
                                "DELIVERED" to 2,
                                "READ" to 3
                            )
                            
                            // Use status with highest priority
                            val serverPriority = statusPriority[serverStatus] ?: 0
                            val clientPriority = statusPriority[clientStatus] ?: 0
                            
                            if (clientPriority > serverPriority) {
                                result[key] = clientStatus
                            }
                        }
                    }
                    // For timestamps, counters, etc., keep server values
                    "timestamp", "updatedAt", "createdAt" -> {
                        // Keep server value
                    }
                    // For most other fields, client wins if updating
                    else -> {
                        if (value != null) {
                            result[key] = value
                        }
                    }
                }
            }
            
            return result
        }
    }
}
