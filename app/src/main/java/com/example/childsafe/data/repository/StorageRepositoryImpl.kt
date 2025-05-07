package com.example.childsafe.data.repository

import android.net.Uri
import com.example.childsafe.domain.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StorageRepository that interacts with Firebase Storage
 * Handles file uploads and management
 */
@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : StorageRepository {

    /**
     * Uploads an image file to Firebase Storage
     */
    override suspend fun uploadImage(
        imageUri: Uri,
        path: String,
        onProgressChanged: (Float) -> Unit
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        val filename = "${UUID.randomUUID()}.jpg"
        val fullPath = "$path/$filename"
        
        val storageRef = storage.reference.child(fullPath)
        
        return uploadFileWithProgress(storageRef, imageUri, onProgressChanged)
    }

    /**
     * Uploads an audio file to Firebase Storage
     */
    override suspend fun uploadAudio(
        audioUri: Uri,
        path: String,
        onProgressChanged: (Float) -> Unit
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        val filename = "${UUID.randomUUID()}.mp3"
        val fullPath = "$path/$filename"
        
        val storageRef = storage.reference.child(fullPath)
        
        return uploadFileWithProgress(storageRef, audioUri, onProgressChanged)
    }
    
    /**
     * Helper method to upload a file with progress tracking
     */
    private suspend fun uploadFileWithProgress(
        storageRef: com.google.firebase.storage.StorageReference,
        uri: Uri,
        onProgressChanged: (Float) -> Unit
    ): String {
        val uploadTask = storageRef.putFile(uri)
        
        // Track upload progress
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
            onProgressChanged(progress)
        }
        
        // Wait for upload to complete
        uploadTask.await()
        
        // Return the download URL
        return storageRef.downloadUrl.await().toString()
    }

    /**
     * Deletes a file from Firebase Storage
     */
    override suspend fun deleteFile(fileUrl: String): Boolean {
        return try {
            // Extract the path from URL
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            storageRef.delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}