package com.memoryuniverse.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_positions",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memory_id"], unique = true)
    ]
)
data class MemoryPositionEntity(
    @PrimaryKey
    @ColumnInfo(name = "memory_id")
    val memoryId: Long,

    @ColumnInfo(name = "pos_x")
    val posX: Float,

    @ColumnInfo(name = "pos_y")
    val posY: Float,

    @ColumnInfo(name = "pos_z")
    val posZ: Float,

    @ColumnInfo(name = "rot_x")
    val rotX: Float = 0f,

    @ColumnInfo(name = "rot_y")
    val rotY: Float = 0f,

    @ColumnInfo(name = "rot_z")
    val rotZ: Float = 0f,

    @ColumnInfo(name = "scale")
    val scale: Float = 1.0f,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false // User manual position lock
)
