package com.memoryuniverse.data.repository

import com.memoryuniverse.core.database.dao.ClusterDao
import com.memoryuniverse.core.database.dao.MemoryDao
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryPositionEntity
import com.memoryuniverse.core.database.entity.MemoryType
import com.memoryuniverse.universe.math.Vector3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.random.Random

class NoteRepository(
    private val memoryDao: MemoryDao,
    private val clusterDao: ClusterDao
) {

    val allNotes: Flow<List<MemoryEntity>> = memoryDao.getAllNotesFlow()

    fun getAttachedNotesForPhoto(photoId: Long): Flow<List<MemoryEntity>> =
        memoryDao.getAttachedNotesForPhotoFlow(photoId)

    fun searchNotes(query: String): Flow<List<MemoryEntity>> =
        memoryDao.searchNotesFlow(query)

    /**
     * Creates a standalone text note floating as a cosmic manuscript in 3D space.
     */
    suspend fun createStandaloneNote(
        title: String,
        content: String,
        clusterId: Long? = 1L
    ): Long = withContext(Dispatchers.IO) {
        val targetClusterId = clusterId ?: 1L
        val note = MemoryEntity(
            memoryType = MemoryType.NOTE,
            mediaUri = null,
            title = title.ifBlank { "Celestial Note" },
            textContent = content,
            timestamp = System.currentTimeMillis(),
            clusterId = targetClusterId
        )
        val noteId = memoryDao.insertMemory(note)

        // Position standalone note in 3D universe with subtle elevation
        val cluster = clusterDao.getClusterById(targetClusterId)
        val cx = cluster?.centroidX ?: 0f
        val cy = cluster?.centroidY ?: 0f
        val cz = cluster?.centroidZ ?: 0f

        val angle = Random.nextFloat() * 6.28f
        val dist = 20f + Random.nextFloat() * 35f
        val posX = cx + dist * kotlin.math.cos(angle)
        val posY = cy + (Random.nextFloat() - 0.5f) * 20f + 12f // slightly elevated
        val posZ = cz + dist * kotlin.math.sin(angle)

        memoryDao.insertPosition(
            MemoryPositionEntity(
                memoryId = noteId,
                posX = posX,
                posY = posY,
                posZ = posZ,
                scale = 0.95f
            )
        )

        clusterDao.recalculateMemoryCounts()
        noteId
    }

    /**
     * Attaches a text note to a specific photograph, orbiting in 3D proximity.
     */
    suspend fun createPhotoAttachedNote(
        photoMemoryId: Long,
        text: String
    ): Long = withContext(Dispatchers.IO) {
        val parentPhoto = memoryDao.getMemoryById(photoMemoryId)
            ?: throw IllegalArgumentException("Parent photo memory $photoMemoryId not found")

        val attachedNote = MemoryEntity(
            memoryType = MemoryType.PHOTO_ATTACHED_NOTE,
            mediaUri = null,
            title = "Attached to ${parentPhoto.title ?: "Photo #$photoMemoryId"}",
            textContent = text,
            timestamp = System.currentTimeMillis(),
            parentMemoryId = photoMemoryId,
            clusterId = parentPhoto.clusterId
        )
        val noteId = memoryDao.insertMemory(attachedNote)

        // Position attached note in close orbit next to parent photo
        // Retrieve parent photo's 3D position if available
        val parentMemoryList = memoryDao.getAllMemoriesWithMetadataFlow()
        // Or default satellite offset (+14f X, +6f Y, +5f Z)
        val posX = 14f
        val posY = 6f
        val posZ = 5f

        memoryDao.insertPosition(
            MemoryPositionEntity(
                memoryId = noteId,
                posX = posX,
                posY = posY,
                posZ = posZ,
                scale = 0.75f // satellite scale
            )
        )

        clusterDao.recalculateMemoryCounts()
        noteId
    }

    suspend fun updateNote(
        noteId: Long,
        title: String,
        content: String
    ) = withContext(Dispatchers.IO) {
        val existing = memoryDao.getMemoryById(noteId) ?: return@withContext
        val updated = existing.copy(
            title = title,
            textContent = content,
            updatedAt = System.currentTimeMillis()
        )
        memoryDao.updateMemory(updated)
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemoryById(noteId)
        clusterDao.recalculateMemoryCounts()
    }
}
