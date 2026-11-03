package com.memoryuniverse.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class RelationshipType {
    AI_DISCOVERED,
    USER_CONFIRMED,
    USER_REJECTED
}

@Entity(
    tableName = "memory_relationships",
    primaryKeys = ["source_memory_id", "target_memory_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_memory_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["target_memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["source_memory_id"]),
        Index(value = ["target_memory_id"]),
        Index(value = ["similarity_weight"])
    ]
)
data class MemoryRelationshipEntity(
    @ColumnInfo(name = "source_memory_id")
    val sourceMemoryId: Long,

    @ColumnInfo(name = "target_memory_id")
    val targetMemoryId: Long,

    @ColumnInfo(name = "similarity_weight")
    val similarityWeight: Float, // 0.0 to 1.0

    @ColumnInfo(name = "relationship_type")
    val relationshipType: RelationshipType = RelationshipType.AI_DISCOVERED,

    @ColumnInfo(name = "semantic_sim")
    val semanticSim: Float = 0f,

    @ColumnInfo(name = "temporal_sim")
    val temporalSim: Float = 0f,

    @ColumnInfo(name = "geo_sim")
    val geoSim: Float = 0f,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
