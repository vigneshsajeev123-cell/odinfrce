package com.memoryuniverse.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clusters",
    indices = [
        Index(value = ["name"]),
        Index(value = ["memory_count"])
    ]
)
data class ClusterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "user_renamed")
    val userRenamed: Boolean = false,

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#8B5CF6", // Default Cosmic Violet

    @ColumnInfo(name = "centroid_x")
    val centroidX: Float = 0f,

    @ColumnInfo(name = "centroid_y")
    val centroidY: Float = 0f,

    @ColumnInfo(name = "centroid_z")
    val centroidZ: Float = 0f,

    @ColumnInfo(name = "radius")
    val radius: Float = 50f,

    @ColumnInfo(name = "memory_count")
    val memoryCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
