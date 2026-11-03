package com.memoryuniverse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.memoryuniverse.core.database.entity.MemoryEmbeddingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {

    @Query("SELECT COUNT(*) FROM memory_embeddings")
    fun getProcessedCountFlow(): Flow<Int>

    @Query("SELECT * FROM memory_embeddings WHERE memory_id = :memoryId LIMIT 1")
    suspend fun getEmbeddingByMemoryId(memoryId: Long): MemoryEmbeddingEntity?

    @Query("SELECT * FROM memory_embeddings")
    suspend fun getAllEmbeddings(): List<MemoryEmbeddingEntity>

    @Query("""
        SELECT id FROM memories 
        WHERE id NOT IN (SELECT memory_id FROM memory_embeddings)
        AND is_excluded = 0
    """)
    suspend fun getUnprocessedMemoryIds(): List<Long>

    @Query("""
        SELECT memory_id FROM memory_embeddings 
        WHERE extracted_keywords LIKE '%' || :keyword || '%' 
           OR ocr_text LIKE '%' || :keyword || '%'
    """)
    suspend fun searchMemoryIdsByKeyword(keyword: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: MemoryEmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<MemoryEmbeddingEntity>)

    @Query("DELETE FROM memory_embeddings WHERE memory_id = :memoryId")
    suspend fun deleteEmbedding(memoryId: Long)
}
