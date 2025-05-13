package com.example.childsafe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for storing offline messages
 */
@Database(entities = [OfflineMessage::class], version = 1, exportSchema = false)
@TypeConverters(OfflineMessageConverters::class)
abstract class OfflineMessageDatabase : RoomDatabase() {
    abstract fun offlineMessageDao(): OfflineMessageDao
}
