package com.memoryuniverse

import android.app.Application
import com.memoryuniverse.core.database.MemoryDatabase
import com.memoryuniverse.data.media.MediaStoreFetcher
import com.memoryuniverse.data.repository.PhotoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Memory Universe Application Class.
 * Initializes offline local database and repositories.
 * Operates strictly offline with zero remote telemetry or backend.
 */
class MemoryUniverseApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { MemoryDatabase.getInstance(this) }
    val mediaStoreFetcher by lazy { MediaStoreFetcher(contentResolver) }
    val photoRepository by lazy {
        PhotoRepository(
            memoryDao = database.memoryDao(),
            clusterDao = database.clusterDao(),
            mediaStoreFetcher = mediaStoreFetcher,
            scope = applicationScope
        )
    }
    val noteRepository by lazy {
        com.memoryuniverse.data.repository.NoteRepository(
            memoryDao = database.memoryDao(),
            clusterDao = database.clusterDao()
        )
    }
    val memoryAiProcessor by lazy {
        com.memoryuniverse.ai.processor.MemoryAiProcessor(
            context = this,
            memoryDao = database.memoryDao(),
            embeddingDao = database.embeddingDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Schedule periodic battery-aware background synchronization
        com.memoryuniverse.workers.SyncScheduler.schedulePeriodicSync(this)
    }

    companion object {
        lateinit var instance: MemoryUniverseApp
            private set
    }
}
