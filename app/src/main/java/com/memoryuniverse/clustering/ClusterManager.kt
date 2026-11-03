package com.memoryuniverse.clustering

import android.content.Context
import com.memoryuniverse.ai.MultimodalEmbeddingEngine
import com.memoryuniverse.clustering.community.LeidenCommunityDetector
import com.memoryuniverse.clustering.graph.MemoryGraphNode
import com.memoryuniverse.clustering.graph.SimilarityGraphBuilder
import com.memoryuniverse.clustering.graph.WeightedEdge
import com.memoryuniverse.clustering.naming.ClusterNamingEngine
import com.memoryuniverse.core.database.MemoryDatabase
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryRelationshipEntity
import com.memoryuniverse.core.database.entity.RelationshipType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level coordinator for offline automatic community detection,
 * similarity graph synchronization, and user-controlled clustering operations.
 */
class ClusterManager(
    private val database: MemoryDatabase,
    private val embeddingEngine: MultimodalEmbeddingEngine = MultimodalEmbeddingEngine()
) {

    private val graphBuilder = SimilarityGraphBuilder(embeddingEngine)
    private val communityDetector = LeidenCommunityDetector()
    private val namingEngine = ClusterNamingEngine()

    private val memoryDao = database.memoryDao()
    private val clusterDao = database.clusterDao()
    private val embeddingDao = database.embeddingDao()
    private val relationshipDao = database.relationshipDao()

    /**
     * Executes automatic clustering pipeline across all or newly added memories.
     * Preserves user-locked nodes and user-renamed cluster titles.
     */
    suspend fun runAutomaticClustering(forceFullRebuild: Boolean = false): ClusteringStats = withContext(Dispatchers.IO) {
        val allMemories = memoryDao.getAllMemoriesList()
        if (allMemories.isEmpty()) {
            return@withContext ClusteringStats(0, 0, 0, 0.0)
        }

        // Build Graph Nodes
        val nodeMap = mutableMapOf<Long, MemoryGraphNode>()
        val lockedNodeMap = mutableMapOf<Long, Long>()
        val currentPartitions = mutableMapOf<Long, Long>()

        for (memory in allMemories) {
            val embeddingEntity = embeddingDao.getEmbeddingForMemory(memory.id)
            val keywords = embeddingEntity?.keywordsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

            nodeMap[memory.id] = MemoryGraphNode(
                memory = memory,
                metadata = null, // Will use photo metadata if available
                embedding = embeddingEntity,
                keywords = keywords
            )

            if (memory.isLocked && memory.clusterId != null) {
                lockedNodeMap[memory.id] = memory.clusterId
            }
            if (memory.clusterId != null) {
                currentPartitions[memory.id] = memory.clusterId
            }
        }

        // Retrieve existing manual relationships
        val manualEdges = relationshipDao.getAllRelationships()
            .filter { it.relationshipType != RelationshipType.AI_DISCOVERED }

        // 1. Build weighted graph
        val edges = graphBuilder.buildGraph(nodeMap.values.toList(), manualEdges)

        // Persist AI-discovered edges
        val edgeEntities = edges.map { edge ->
            MemoryRelationshipEntity(
                sourceMemoryId = edge.sourceId,
                targetMemoryId = edge.targetId,
                similarityWeight = edge.totalWeight,
                semanticSimilarity = edge.semanticSim,
                temporalSimilarity = edge.temporalSim,
                geoSimilarity = edge.geoSim,
                keywordSimilarity = edge.keywordSim,
                relationshipType = edge.type
            )
        }
        relationshipDao.insertRelationships(edgeEntities)

        // 2. Modularity community detection (Leiden/Louvain)
        val clusteringResult = communityDetector.detectCommunities(
            nodeIds = allMemories.map { it.id },
            edges = edges,
            lockedNodes = lockedNodeMap,
            currentPartitions = if (forceFullRebuild) emptyMap() else currentPartitions
        )

        // 3. Synchronize clusters with Room database
        val existingClusters = clusterDao.getAllClusters().associateBy { it.id }.toMutableMap()
        val updatedClusterIds = mutableSetOf<Long>()

        for ((communityId, memberIds) in clusteringResult.clusterMembers) {
            val clusterMemories = memberIds.mapNotNull { nodeMap[it]?.memory }
            val clusterKeywords = memberIds.mapNotNull { nodeMap[it]?.keywords?.toList() }
            val clusterOcr = memberIds.mapNotNull { nodeMap[it]?.embedding?.ocrText }

            val existingCluster = existingClusters[communityId]
            val clusterName = namingEngine.generateClusterName(
                existingCluster = existingCluster,
                memories = clusterMemories,
                metadataList = emptyList(),
                keywordsList = clusterKeywords,
                ocrTexts = clusterOcr
            )

            val color = existingCluster?.colorHex ?: namingEngine.suggestClusterColor(communityId, clusterName)

            val clusterEntity = existingCluster?.copy(
                name = clusterName,
                memoryCount = clusterMemories.size,
                updatedAt = System.currentTimeMillis()
            ) ?: ClusterEntity(
                id = if (communityId > 0 && communityId < 100000) communityId else 0,
                name = clusterName,
                userRenamed = false,
                colorHex = color,
                memoryCount = clusterMemories.size
            )

            val savedClusterId = clusterDao.insertCluster(clusterEntity)
            val effectiveClusterId = if (clusterEntity.id != 0L) clusterEntity.id else savedClusterId
            updatedClusterIds.add(effectiveClusterId)

            // Update memories belonging to this cluster (respecting isLocked)
            for (memId in memberIds) {
                val node = nodeMap[memId]
                if (node != null && !node.memory.isLocked) {
                    memoryDao.updateCluster(memId, effectiveClusterId)
                }
            }
        }

        // Dissolve isolated memories (remove from clusters)
        for (isolatedId in clusteringResult.isolatedMemoryIds) {
            val node = nodeMap[isolatedId]
            if (node != null && !node.memory.isLocked) {
                memoryDao.updateCluster(isolatedId, null)
            }
        }

        // Recalculate cluster member counts
        clusterDao.recalculateMemoryCounts()

        ClusteringStats(
            totalMemories = allMemories.size,
            totalClusters = clusteringResult.clusterMembers.size,
            isolatedMemories = clusteringResult.isolatedMemoryIds.size,
            modularity = clusteringResult.modularity
        )
    }

    // --- USER CONTROLS ---

    suspend fun renameCluster(clusterId: Long, newName: String) = withContext(Dispatchers.IO) {
        clusterDao.renameCluster(clusterId, newName)
    }

    suspend fun mergeClusters(targetClusterId: Long, sourceClusterId: Long) = withContext(Dispatchers.IO) {
        if (targetClusterId == sourceClusterId) return@withContext
        memoryDao.reassignCluster(oldClusterId = sourceClusterId, newClusterId = targetClusterId)
        clusterDao.deleteClusterById(sourceClusterId)
        clusterDao.recalculateMemoryCounts()
    }

    suspend fun splitCluster(
        sourceClusterId: Long,
        memoryIdsToSplit: List<Long>,
        newClusterName: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val title = newClusterName ?: "Sub-Cluster ${System.currentTimeMillis() % 1000}"
        val newCluster = ClusterEntity(
            name = title,
            userRenamed = true,
            colorHex = namingEngine.suggestClusterColor(System.currentTimeMillis(), title),
            memoryCount = memoryIdsToSplit.size
        )
        val newClusterId = clusterDao.insertCluster(newCluster)

        for (id in memoryIdsToSplit) {
            memoryDao.updateMemoryClusterForced(memoryId = id, clusterId = newClusterId, isLocked = true)
        }

        clusterDao.recalculateMemoryCounts()
        newClusterId
    }

    suspend fun moveMemoryToCluster(
        memoryId: Long,
        newClusterId: Long?,
        lockAssignment: Boolean = true
    ) = withContext(Dispatchers.IO) {
        memoryDao.updateMemoryClusterForced(memoryId, newClusterId, lockAssignment)
        clusterDao.recalculateMemoryCounts()
    }

    suspend fun excludeMemory(memoryId: Long, isExcluded: Boolean = true) = withContext(Dispatchers.IO) {
        memoryDao.excludeMemory(memoryId, isExcluded)
        clusterDao.recalculateMemoryCounts()
    }

    suspend fun setMemoryLocked(memoryId: Long, isLocked: Boolean) = withContext(Dispatchers.IO) {
        memoryDao.setLocked(memoryId, isLocked)
    }

    companion object {
        fun create(context: Context): ClusterManager {
            val db = MemoryDatabase.getInstance(context)
            return ClusterManager(db)
        }
    }
}

data class ClusteringStats(
    val totalMemories: Int,
    val totalClusters: Int,
    val isolatedMemories: Int,
    val modularity: Double
)
