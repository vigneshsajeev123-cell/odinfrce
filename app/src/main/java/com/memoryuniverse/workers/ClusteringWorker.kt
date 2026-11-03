package com.memoryuniverse.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.memoryuniverse.clustering.ClusterManager
import com.memoryuniverse.universe.positioning.ForceDirectedUniverseLayout
import com.memoryuniverse.core.database.MemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker for performing offline graph modularity clustering
 * and 3D spatial layout repositioning while device is charging or idle.
 */
class ClusteringWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = MemoryDatabase.getInstance(applicationContext)
            val clusterManager = ClusterManager.create(applicationContext)
            val layoutEngine = ForceDirectedUniverseLayout()

            // 1. Modularity clustering
            val stats = clusterManager.runAutomaticClustering(forceFullRebuild = false)

            // 2. Spatial layout recalculation
            val memories = database.memoryDao().getAllMemoriesList()
            val clusters = database.clusterDao().getAllClusters()
            val existingPositions = database.memoryDao().getAllPositions().associateBy { it.memoryId }
            val relationships = database.relationshipDao().getAllRelationships().map { rel ->
                com.memoryuniverse.clustering.graph.WeightedEdge(
                    sourceId = rel.sourceMemoryId,
                    targetId = rel.targetMemoryId,
                    totalWeight = rel.similarityWeight,
                    semanticSim = rel.semanticSimilarity,
                    temporalSim = rel.temporalSimilarity,
                    geoSim = rel.geoSimilarity,
                    keywordSim = rel.keywordSimilarity,
                    type = rel.relationshipType
                )
            }

            val layoutResult = layoutEngine.computeLayout(
                memories = memories,
                clusters = clusters,
                existingPositions = existingPositions,
                relationships = relationships
            )

            database.memoryDao().insertPositions(layoutResult.positions)
            for (c in layoutResult.clusters) {
                database.clusterDao().updateCentroid(c.id, c.centroidX, c.centroidY, c.centroidZ, c.radius)
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
        const val WORK_NAME = "ClusteringWorker"
    }
}
