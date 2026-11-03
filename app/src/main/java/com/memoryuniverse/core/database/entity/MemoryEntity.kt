package com.memoryuniverse.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MemoryType {
    PHOTO,
    NOTE,
    PHOTO_ATTACHED_NOTE
}

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["media_uri"], unique = true),
        Index(value = ["cluster_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["is_favorite"]),
        Index(value = ["memory_type"])
    ]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "memory_type")
    val memoryType: MemoryType = MemoryType.PHOTO,

    @ColumnInfo(name = "media_uri")
    val mediaUri: String?, // Content URI for photos (e.g. content://media/external/images/media/1234)

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "text_content")
    val textContent: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Epoch millis

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "cluster_id")
    val clusterId: Long? = null,

    @ColumnInfo(name = "parent_memory_id")
    val parentMemoryId: Long? = null, // For photo-attached notes

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_excluded")
    val isExcluded: Boolean = false,

    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false, // User manual lock against AI reassignment

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
