package com.example.childsafe.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for offline messages
 */
@Dao
interface OfflineMessageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: OfflineMessage)
    
    @Query("SELECT * FROM offline_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<OfflineMessage>>
    
    @Query("SELECT * FROM offline_messages WHERE status = :status ORDER BY timestamp ASC")
    fun getMessagesByStatus(status: OfflineMessageStatus): Flow<List<OfflineMessage>>
    
    @Update
    suspend fun updateMessage(message: OfflineMessage)
    
    @Delete
    suspend fun deleteMessage(message: OfflineMessage)
    
    @Query("DELETE FROM offline_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)
    
    @Query("UPDATE offline_messages SET status = :newStatus, retryCount = :retryCount, lastRetryTimestamp = :timestamp WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: OfflineMessageStatus, retryCount: Int, timestamp: Long)
}
