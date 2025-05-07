package com.example.childsafe.domain.repository

import android.net.Uri

/**
 * Repository interface for storage operations
 * Handles uploading and managing files in cloud storage
 */
interface StorageRepository {

    /**
     * Uploads an image file to storage
     * @param imageUri URI of the image to upload
     * @param path Storage path where the image should be stored
     * @param onProgressChanged Callback to report upload progress (0.0f to 1.0f)
     * @return URL of the uploaded image
     */
    suspend fun uploadImage(
        imageUri: Uri,
        path: String,
        onProgressChanged: (Float) -> Unit = {}
    ): String

    /**
     * Uploads an audio file to storage
     * @param audioUri URI of the audio file to upload
     * @param path Storage path where the audio should be stored
     * @param onProgressChanged Callback to report upload progress (0.0f to 1.0f)
     * @return URL of the uploaded audio file
     */
    suspend fun uploadAudio(
        audioUri: Uri,
        path: String,
        onProgressChanged: (Float) -> Unit = {}
    ): String

    /**
     * Deletes a file from storage
     * @param fileUrl URL of the file to delete
     * @return Whether the deletion was successful
     */
    suspend fun deleteFile(fileUrl: String): Boolean
}