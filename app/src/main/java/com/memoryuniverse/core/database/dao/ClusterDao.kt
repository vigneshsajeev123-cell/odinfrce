package com.memoryuniverse.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.memoryuniverse.core.database.entity.ClusterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterDao {

    @Query("SELECT * FROM clusters ORDER BY memory_count DESC")
    fun getAllClustersFlow(): Flow<List<ClusterEntity>>

    @Query("SELECT * FROM clusters WHERE id = :id LIMIT 1")
    suspend fun getClusterById(id: Long): ClusterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: ClusterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClusters(clusters: List<ClusterEntity>): List<Long>

    @Update
    suspend fun updateCluster(cluster: ClusterEntity)

    @Query("UPDATE clusters SET name = :newName, user_renamed = 1, updated_at = :updatedAt WHERE id = :clusterId")
    suspend fun renameCluster(clusterId: Long, newName: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteCluster(cluster: ClusterEntity)

    @Query("DELETE FROM clusters WHERE id = :id")
    suspend fun deleteClusterById(id: Long)

    @Query("SELECT * FROM clusters ORDER BY memory_count DESC")
    suspend fun getAllClusters(): List<ClusterEntity>

    @Query("UPDATE clusters SET centroid_x = :cx, centroid_y = :cy, centroid_z = :cz, radius = :radius WHERE id = :clusterId")
    suspend fun updateCentroid(clusterId: Long, cx: Float, cy: Float, cz: Float, radius: Float)

    @Query("""
        UPDATE clusters 
        SET memory_count = (SELECT COUNT(*) FROM memories WHERE cluster_id = clusters.id AND is_excluded = 0)
    """)
    suspend fun recalculateMemoryCounts()
}
