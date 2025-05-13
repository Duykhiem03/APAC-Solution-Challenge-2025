package com.example.childsafe.concurrency

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling versioned document updates with conflict resolution
 */
@Singleton
class DocumentVersioningService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val conflictResolutionService: ConflictResolutionService
) {
    /**
     * Maximum number of retry attempts for failed transactions
     */
    private val MAX_RETRIES = 5
    
    /**
     * Updates a versioned document with optimistic concurrency control
     *
     * @param docRef The document reference to update
     * @param updates Map of field updates to apply
     * @param expectedVersion The expected current version of the document
     * @param resolver Optional resolver for handling conflicts
     * @return The updated document data
     * @throws ConcurrencyException If the document version doesn't match after retries
     */
    suspend fun updateVersionedDocument(
        docRef: DocumentReference,
        updates: Map<String, Any?>,
        expectedVersion: Long,
        resolver: ConflictResolutionService.ConflictResolver<Map<String, Any?>> = 
            ConflictResolutionService.MapMergeResolver()
    ): Map<String, Any?> {
        var retryCount = 0
        var lastException: Exception? = null
        
        while (retryCount < MAX_RETRIES) {
            try {
                return firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    
                    if (!snapshot.exists()) {
                        throw DocumentNotFoundException("Document ${docRef.path} not found")
                    }
                    
                    val currentData = snapshot.data ?: emptyMap()
                    val currentVersion = currentData["version"] as? Long ?: 0
                    
                    // Check if the version matches our expectation
                    if (currentVersion != expectedVersion) {
                        // Resolve the conflict
                        val resolvedData = conflictResolutionService.resolveConflict(
                            expectedVersion,
                            currentVersion,
                            updates,
                            currentData,
                            resolver
                        )
                        
                        // Increment version and update
                        val finalUpdates = resolvedData.toMutableMap().apply {
                            put("version", currentVersion + 1)
                        }
                        
                        transaction.set(docRef, finalUpdates)
                        return@runTransaction finalUpdates
                    } else {
                        // No conflict, increment version and update
                        val finalUpdates = updates.toMutableMap().apply {
                            put("version", expectedVersion + 1)
                        }
                        
                        transaction.set(docRef, finalUpdates)
                        return@runTransaction finalUpdates
                    }
                }.await()
            } catch (e: FirebaseFirestoreException) {
                lastException = e
                Timber.w(e, "Transaction failed (attempt ${retryCount + 1}), retrying...")
                retryCount++
                kotlinx.coroutines.delay(exponentialBackoff(retryCount))
            }
        }
        
        throw ConcurrencyException("Failed to update document after $MAX_RETRIES attempts", lastException)
    }
    
    /**
     * Creates a new versioned document with conflict prevention
     */
    suspend fun createVersionedDocument(
        docRef: DocumentReference,
        data: Map<String, Any?>
    ): Map<String, Any?> {
        val initialData = data.toMutableMap().apply {
            put("version", 1L) // Initial version
        }
        
        try {
            docRef.set(initialData).await()
            return initialData
        } catch (e: Exception) {
            Timber.e(e, "Error creating versioned document")
            throw e
        }
    }
    
    /**
     * Calculates exponential backoff time for retries
     */
    private fun exponentialBackoff(retryCount: Int): Long {
        return 300L * (1 shl retryCount) // 300ms, 600ms, 1200ms, etc.
    }
    
    /**
     * Exception thrown when a document doesn't exist
     */
    class DocumentNotFoundException(message: String) : Exception(message)
    
    /**
     * Exception thrown when concurrency control fails after retries
     */
    class ConcurrencyException(message: String, cause: Exception?) : Exception(message, cause)
}
