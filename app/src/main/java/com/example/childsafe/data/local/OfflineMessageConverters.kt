package com.example.childsafe.data.local

import androidx.room.TypeConverter
import com.example.childsafe.data.model.MessageType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database to handle complex types
 */
class OfflineMessageConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromMessageTypeToString(messageType: MessageType): String {
        return messageType.name
    }

    @TypeConverter
    fun toMessageTypeFromString(messageTypeName: String): MessageType {
        return try {
            MessageType.valueOf(messageTypeName)
        } catch (e: Exception) {
            MessageType.TEXT
        }
    }

    @TypeConverter
    fun fromOfflineMessageStatusToString(status: OfflineMessageStatus): String {
        return status.name
    }

    @TypeConverter
    fun toOfflineMessageStatusFromString(statusName: String): OfflineMessageStatus {
        return try {
            OfflineMessageStatus.valueOf(statusName)
        } catch (e: Exception) {
            OfflineMessageStatus.PENDING
        }
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, Any>?): String? {
        return map?.let {
            gson.toJson(it)
        }
    }

    @TypeConverter
    fun toMapFromString(json: String?): Map<String, Any>? {
        return json?.let {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson<Map<String, Any>>(it, type)
        }
    }
}
