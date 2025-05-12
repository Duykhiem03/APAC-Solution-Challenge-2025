package com.example.childsafe.concurrency

import com.example.childsafe.data.local.OfflineMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for monitoring and resolving data consistency issues
 */
@Singleton
class ConsistencyMonitorService @Inject constructor() {

    // Flow for reporting data consistency events
    private val _consistencyEvents = MutableSharedFlow<ConsistencyEvent>(extraBufferCapacity = 10)
    val consistencyEvents: Flow<ConsistencyEvent> = _consistencyEvents

    // Track monitored documents for conflict potential
    private val monitoredDocuments = mutableMapOf<String, DocumentVersionInfo>()

    /**
     * Register a document for monitoring
     * @param docId The document ID
     * @param path The document path
     * @param version The current version number
     */
    suspend fun trackDocument(docId: String, path: String, version: Long) {
        val info = DocumentVersionInfo(docId, path, version, System.currentTimeMillis())
        monitoredDocuments[docId] = info
        _consistencyEvents.emit(ConsistencyEvent.DocumentTracked(info))
        Timber.d("Started tracking document: $path, version: $version")
    }

    /**
     * Register a version update for a tracked document
     * @param docId The document ID
     * @param oldVersion The previous version
     * @param newVersion The new version
     * @param wasConflict Whether a conflict was detected and resolved
     */
    suspend fun recordVersionUpdate(
        docId: String,
        path: String,
        oldVersion: Long,
        newVersion: Long,
        wasConflict: Boolean
    ) {
        val previousInfo = monitoredDocuments[docId]
        val info = DocumentVersionInfo(docId, path, newVersion, System.currentTimeMillis())
        monitoredDocuments[docId] = info

        if (wasConflict) {
            val event = ConsistencyEvent.ConflictDetected(
                info, 
                previousInfo?.version ?: oldVersion,
                "Version conflict detected and resolved"
            )
            _consistencyEvents.emit(event)
            Timber.w(
                "Conflict detected for document $path: local version $oldVersion " +
                "vs. remote version ${previousInfo?.version ?: "unknown"}, " +
                "resolved to version $newVersion"
            )
        } else {
            _consistencyEvents.emit(ConsistencyEvent.VersionUpdated(info))
            Timber.d("Updated document version: $path, $oldVersion -> $newVersion")
        }
    }

    /**
     * Record a failed operation due to consistency issues
     */
    suspend fun recordFailedOperation(
        docId: String,
        path: String,
        operation: String,
        error: Throwable
    ) {
        val event = ConsistencyEvent.OperationFailed(
            docId, path, operation, error.message ?: "Unknown error"
        )
        _consistencyEvents.emit(event)
        Timber.e(error, "Operation $operation failed on $path")
    }

    /**
     * Log an offline operation for later synchronization
     */
    suspend fun logOfflineOperation(message: OfflineMessage) {
        val event = ConsistencyEvent.OfflineOperation(
            message.id, 
            "messages/${message.id}", 
            "Queued for later synchronization"
        )
        _consistencyEvents.emit(event)
        Timber.d("Queued offline operation: ${message.id}, type: ${message.messageType}")
    }

    /**
     * Log a background operation (worker, service, etc.)
     */
    suspend fun logBackgroundOperation(operationName: String, description: String) {
        val event = ConsistencyEvent.BackgroundOperation(
            operationName,
            description,
            System.currentTimeMillis()
        )
        _consistencyEvents.emit(event)
        Timber.d("Background operation: $operationName - $description")
    }

    /**
     * Data class for tracking document version information
     */
    data class DocumentVersionInfo(
        val docId: String,
        val path: String,
        val version: Long,
        val timestamp: Long
    )

    /**
     * Sealed class for consistency-related events
     */
    sealed class ConsistencyEvent {
        data class DocumentTracked(val info: DocumentVersionInfo) : ConsistencyEvent()
        data class VersionUpdated(val info: DocumentVersionInfo) : ConsistencyEvent()
        data class ConflictDetected(
            val info: DocumentVersionInfo,
            val expectedVersion: Long,
            val message: String
        ) : ConsistencyEvent()
        data class OperationFailed(
            val docId: String,
            val path: String,
            val operation: String,
            val error: String
        ) : ConsistencyEvent()
        data class OfflineOperation(
            val docId: String,
            val path: String,
            val details: String
        ) : ConsistencyEvent()
        data class BackgroundOperation(
            val operationName: String,
            val description: String,
            val timestamp: Long
        ) : ConsistencyEvent()
    }
}
