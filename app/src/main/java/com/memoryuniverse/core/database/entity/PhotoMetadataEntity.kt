package com.memoryuniverse.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_metadata",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memory_id"], unique = true),
        Index(value = ["media_store_id"])
    ]
)
data class PhotoMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "memory_id")
    val memoryId: Long,

    @ColumnInfo(name = "media_store_id")
    val mediaStoreId: Long,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "width")
    val width: Int,

    @ColumnInfo(name = "height")
    val height: Int,

    @ColumnInfo(name = "orientation")
    val orientation: Int,

    @ColumnInfo(name = "camera_make")
    val cameraMake: String? = null,

    @ColumnInfo(name = "camera_model")
    val cameraModel: String? = null,

    @ColumnInfo(name = "focal_length")
    val focalLength: Double? = null,

    @ColumnInfo(name = "iso")
    val iso: Int? = null,

    @ColumnInfo(name = "exposure_time")
    val exposureTime: Double? = null,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null
)
