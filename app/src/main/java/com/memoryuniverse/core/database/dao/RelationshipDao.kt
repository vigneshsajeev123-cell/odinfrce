package com.memoryuniverse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.memoryuniverse.core.database.entity.MemoryRelationshipEntity
import com.memoryuniverse.core.database.entity.RelationshipType
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {

    @Query("SELECT * FROM memory_relationships WHERE source_memory_id = :memoryId OR target_memory_id = :memoryId ORDER BY similarity_weight DESC")
    fun getRelationshipsForMemoryFlow(memoryId: Long): Flow<List<MemoryRelationshipEntity>>

    @Query("SELECT * FROM memory_relationships WHERE similarity_weight >= :minWeight ORDER BY similarity_weight DESC")
    suspend fun getAllSignificantRelationships(minWeight: Float = 0.40f): List<MemoryRelationshipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: MemoryRelationshipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<MemoryRelationshipEntity>)

    @Query("UPDATE memory_relationships SET relationship_type = :type WHERE (source_memory_id = :sourceId AND target_memory_id = :targetId) OR (source_memory_id = :targetId AND target_memory_id = :sourceId)")
    suspend fun updateRelationshipType(sourceId: Long, targetId: Long, type: RelationshipType)

    @Query("DELETE FROM memory_relationships WHERE (source_memory_id = :sourceId AND target_memory_id = :targetId) OR (source_memory_id = :targetId AND target_memory_id = :sourceId)")
    suspend fun deleteRelationship(sourceId: Long, targetId: Long)

    @Query("DELETE FROM memory_relationships WHERE relationship_type = 'AI_DISCOVERED'")
    suspend fun clearAiRelationships()
}
