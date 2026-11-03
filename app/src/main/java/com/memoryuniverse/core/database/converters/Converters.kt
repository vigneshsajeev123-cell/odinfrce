package com.memoryuniverse.core.database.converters

import androidx.room.TypeConverter
import com.memoryuniverse.core.database.entity.MemoryType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromMemoryType(type: MemoryType): String {
        return type.name
    }

    @TypeConverter
    fun toMemoryType(value: String): MemoryType {
        return try {
            MemoryType.valueOf(value)
        } catch (e: Exception) {
            MemoryType.PHOTO
        }
    }

    @TypeConverter
    fun fromRelationshipType(type: com.memoryuniverse.core.database.entity.RelationshipType): String {
        return type.name
    }

    @TypeConverter
    fun toRelationshipType(value: String): com.memoryuniverse.core.database.entity.RelationshipType {
        return try {
            com.memoryuniverse.core.database.entity.RelationshipType.valueOf(value)
        } catch (e: Exception) {
            com.memoryuniverse.core.database.entity.RelationshipType.AI_DISCOVERED
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString("|||")
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split("|||")
    }
}
