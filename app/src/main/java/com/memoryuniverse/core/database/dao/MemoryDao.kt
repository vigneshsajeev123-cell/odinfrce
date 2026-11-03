package com.memoryuniverse.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryPositionEntity
import com.memoryuniverse.core.database.entity.MemoryType
import com.memoryuniverse.core.database.entity.PhotoMetadataEntity
import kotlinx.coroutines.flow.Flow

data class MemoryWithMetadata(
    val id: Long,
    val memoryType: MemoryType,
    val mediaUri: String?,
    val title: String?,
    val textContent: String?,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val clusterId: Long?,
    val parentMemoryId: Long?,
    val isFavorite: Boolean,
    val isExcluded: Boolean,
    val isLocked: Boolean,
    val displayName: String?,
    val width: Int?,
    val height: Int?,
    val thumbnailPath: String?,
    val posX: Float?,
    val posY: Float?,
    val posZ: Float?
)

@Dao
interface MemoryDao {

    @Query("SELECT COUNT(*) FROM memories WHERE is_excluded = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM memories WHERE memory_type = 'PHOTO' AND is_excluded = 0")
    fun getPhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM memories WHERE (memory_type = 'NOTE' OR memory_type = 'PHOTO_ATTACHED_NOTE') AND is_excluded = 0")
    fun getNoteCount(): Flow<Int>

    @Query("SELECT * FROM memories WHERE is_excluded = 0 ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("""
        SELECT m.id, m.memory_type as memoryType, m.media_uri as mediaUri, m.title, m.text_content as textContent,
               m.timestamp, m.latitude, m.longitude, m.cluster_id as clusterId, m.parent_memory_id as parentMemoryId,
               m.is_favorite as isFavorite, m.is_excluded as isExcluded, m.is_locked as isLocked,
               p.display_name as displayName, p.width, p.height, p.thumbnail_path as thumbnailPath,
               pos.pos_x as posX, pos.pos_y as posY, pos.pos_z as posZ
        FROM memories m
        LEFT JOIN photo_metadata p ON m.id = p.memory_id
        LEFT JOIN memory_positions pos ON m.id = pos.memory_id
        WHERE m.is_excluded = 0
        ORDER BY m.timestamp DESC
    """)
    fun getAllMemoriesWithMetadataFlow(): Flow<List<MemoryWithMetadata>>

    @Query("SELECT * FROM memories WHERE parent_memory_id = :photoId AND is_excluded = 0 ORDER BY timestamp ASC")
    fun getAttachedNotesForPhotoFlow(photoId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE (memory_type = 'NOTE' OR memory_type = 'PHOTO_ATTACHED_NOTE') AND is_excluded = 0 ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<MemoryEntity>>

    @Query("""
        SELECT * FROM memories 
        WHERE (memory_type = 'NOTE' OR memory_type = 'PHOTO_ATTACHED_NOTE')
        AND is_excluded = 0 
        AND (title LIKE '%' || :query || '%' OR text_content LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchNotesFlow(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories WHERE media_uri = :uri LIMIT 1")
    suspend fun getMemoryByUri(uri: String): MemoryEntity?

    @Query("SELECT media_uri FROM memories WHERE media_uri IS NOT NULL")
    suspend fun getAllIndexedUris(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoMetadata(metadata: PhotoMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoMetadataList(metadataList: List<PhotoMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: MemoryPositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPositions(positions: List<MemoryPositionEntity>)

    @Query("SELECT * FROM memory_positions")
    suspend fun getAllPositions(): List<MemoryPositionEntity>

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET is_favorite = :isFavorite WHERE id = :memoryId")
    suspend fun setFavorite(memoryId: Long, isFavorite: Boolean)

    @Query("UPDATE memories SET cluster_id = :clusterId WHERE id = :memoryId AND is_locked = 0")
    suspend fun updateCluster(memoryId: Long, clusterId: Long?)

    @Query("UPDATE memories SET is_locked = :isLocked WHERE id = :memoryId")
    suspend fun setLocked(memoryId: Long, isLocked: Boolean)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memories WHERE media_uri NOT IN (:validUris) AND memory_type = 'PHOTO'")
    suspend fun removeStaleUris(validUris: List<String>)

    @Query("SELECT * FROM memories WHERE cluster_id = :clusterId AND is_excluded = 0 ORDER BY timestamp DESC")
    fun getMemoriesByClusterIdFlow(clusterId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE cluster_id = :clusterId AND is_excluded = 0 ORDER BY timestamp DESC")
    suspend fun getMemoriesByClusterId(clusterId: Long): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE is_excluded = 0 ORDER BY timestamp DESC")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Query("UPDATE memories SET cluster_id = :clusterId, is_locked = :isLocked WHERE id = :memoryId")
    suspend fun updateMemoryClusterForced(memoryId: Long, clusterId: Long?, isLocked: Boolean)

    @Query("UPDATE memories SET is_excluded = :isExcluded WHERE id = :memoryId")
    suspend fun excludeMemory(memoryId: Long, isExcluded: Boolean)

    @Query("UPDATE memories SET cluster_id = :newClusterId WHERE cluster_id = :oldClusterId")
    suspend fun reassignCluster(oldClusterId: Long, newClusterId: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
