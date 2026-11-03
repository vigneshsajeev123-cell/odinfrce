package com.memoryuniverse.data.repository

import com.memoryuniverse.core.database.dao.ClusterDao
import com.memoryuniverse.core.database.dao.MemoryDao
import com.memoryuniverse.core.database.dao.MemoryWithMetadata
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryPositionEntity
import com.memoryuniverse.core.database.entity.MemoryType
import com.memoryuniverse.core.database.entity.PhotoMetadataEntity
import com.memoryuniverse.data.media.MediaStoreFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PhotoRepository(
    private val memoryDao: MemoryDao,
    private val clusterDao: ClusterDao,
    private val mediaStoreFetcher: MediaStoreFetcher,
    private val scope: CoroutineScope
) {

    val allMemories: Flow<List<MemoryWithMetadata>> = memoryDao.getAllMemoriesWithMetadataFlow()
    val totalCount: Flow<Int> = memoryDao.getTotalCount()
    val photoCount: Flow<Int> = memoryDao.getPhotoCount()
    val noteCount: Flow<Int> = memoryDao.getNoteCount()
    val allClusters: Flow<List<ClusterEntity>> = clusterDao.getAllClustersFlow()

    /**
     * Reconciles device MediaStore images with the local Room database.
     * Efficiently adds newly detected photos without duplicating file data.
     */
    suspend fun syncLocalPhotos(): Int = withContext(Dispatchers.IO) {
        val rawPhotos = mediaStoreFetcher.fetchLocalPhotos()
        val existingUris = memoryDao.getAllIndexedUris().toSet()

        val newPhotos = rawPhotos.filter { it.uri.toString() !in existingUris }
        if (newPhotos.isEmpty()) return@withContext 0

        // Ensure at least one initial cluster exists (e.g. Origin Galaxy)
        val defaultClusterId = getOrCreateDefaultCluster()

        var insertedCount = 0
        newPhotos.forEachIndexed { index, raw ->
            val memory = MemoryEntity(
                memoryType = MemoryType.PHOTO,
                mediaUri = raw.uri.toString(),
                title = raw.displayName,
                timestamp = raw.timestamp,
                latitude = raw.latitude,
                longitude = raw.longitude,
                clusterId = defaultClusterId
            )
            val memoryId = memoryDao.insertMemory(memory)

            val metadata = PhotoMetadataEntity(
                memoryId = memoryId,
                mediaStoreId = raw.mediaStoreId,
                displayName = raw.displayName,
                mimeType = raw.mimeType,
                sizeBytes = raw.sizeBytes,
                width = raw.width,
                height = raw.height,
                orientation = raw.orientation,
                cameraMake = raw.cameraMake,
                cameraModel = raw.cameraModel,
                focalLength = raw.focalLength,
                iso = raw.iso,
                exposureTime = raw.exposureTime
            )
            memoryDao.insertPhotoMetadata(metadata)

            // Initial 3D Universe spiral positioning around cluster origin
            val angle = index * 0.45f
            val distance = 30f + (index * 2.5f)
            val posX = distance * cos(angle)
            val posY = (Random.nextFloat() - 0.5f) * 40f
            val posZ = distance * sin(angle)

            memoryDao.insertPosition(
                MemoryPositionEntity(
                    memoryId = memoryId,
                    posX = posX,
                    posY = posY,
                    posZ = posZ
                )
            )
            insertedCount++
        }

        clusterDao.recalculateMemoryCounts()
        insertedCount
    }

    private suspend fun getOrCreateDefaultCluster(): Long {
        val existing = clusterDao.getClusterById(1)
        if (existing != null) return existing.id

        val originCluster = ClusterEntity(
            id = 1,
            name = "Origin Nebula",
            userRenamed = false,
            colorHex = "#8B5CF6",
            centroidX = 0f,
            centroidY = 0f,
            centroidZ = 0f,
            radius = 120f,
            memoryCount = 0
        )
        return clusterDao.insertCluster(originCluster)
    }

    suspend fun toggleFavorite(memoryId: Long, currentFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            memoryDao.setFavorite(memoryId, !currentFavorite)
        }
    }

    suspend fun deleteMemory(memoryId: Long) {
        withContext(Dispatchers.IO) {
            memoryDao.deleteMemoryById(memoryId)
            clusterDao.recalculateMemoryCounts()
        }
    }
}
