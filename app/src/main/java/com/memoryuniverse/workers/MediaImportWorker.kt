package com.memoryuniverse.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.memoryuniverse.ai.MemoryAiProcessor
import com.memoryuniverse.core.database.MemoryDatabase
import com.memoryuniverse.core.media.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Battery-conscious WorkManager worker for scanning external storage,
 * indexing newly captured photos, and generating on-device multimodal embeddings.
 */
class MediaImportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val photoRepository = PhotoRepository.create(applicationContext)
            val database = MemoryDatabase.getInstance(applicationContext)
            val aiProcessor = MemoryAiProcessor.create(applicationContext)

            // 1. Sync external photos into Room
            val syncResult = photoRepository.syncExternalPhotos()

            // 2. Process AI embeddings for un-indexed memories in batches
            val allMemories = database.memoryDao().getAllMemoriesList()
            val unindexed = allMemories.filter { mem ->
                database.embeddingDao().getEmbeddingForMemory(mem.id) == null
            }

            if (unindexed.isNotEmpty()) {
                aiProcessor.processMemories(unindexed.take(50)) // Throttle to 50 items per job to conserve battery
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "MediaImportWorker"
    }
}
