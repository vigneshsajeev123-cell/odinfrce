package com.memoryuniverse.clustering.graph

import com.memoryuniverse.ai.MultimodalEmbeddingEngine
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryEmbeddingEntity
import com.memoryuniverse.core.database.entity.MemoryRelationshipEntity
import com.memoryuniverse.core.database.entity.PhotoMetadataEntity
import com.memoryuniverse.core.database.entity.RelationshipType
import kotlin.math.*

/**
 * Weights configuration for combining multi-modal similarity signals.
 * Missing modalities (e.g. GPS, timestamp) are dynamically excluded and weights
 * are renormalized so missing data is never treated as negative evidence.
 */
data class SimilarityWeights(
    val semanticWeight: Float = 0.50f,
    val temporalWeight: Float = 0.20f,
    val geographicWeight: Float = 0.15f,
    val keywordWeight: Float = 0.15f,
    val minEdgeThreshold: Float = 0.38f,
    val maxNeighborsPerNode: Int = 15
)

/**
 * Graph node encapsulating a memory, its optional photo metadata, keywords, and embedding.
 */
data class MemoryGraphNode(
    val memory: MemoryEntity,
    val metadata: PhotoMetadataEntity?,
    val embedding: MemoryEmbeddingEntity?,
    val keywords: Set<String>
)

/**
 * Weighted edge between two memories.
 */
data class WeightedEdge(
    val sourceId: Long,
    val targetId: Long,
    val totalWeight: Float,
    val semanticSim: Float,
    val temporalSim: Float,
    val geoSim: Float,
    val keywordSim: Float,
    val type: RelationshipType = RelationshipType.AI_DISCOVERED
)

class SimilarityGraphBuilder(
    private val embeddingEngine: MultimodalEmbeddingEngine,
    private val weights: SimilarityWeights = SimilarityWeights()
) {

    /**
     * Builds the similarity graph edges across the provided nodes using k-NN candidate retrieval.
     */
    fun buildGraph(
        nodes: List<MemoryGraphNode>,
        existingManualEdges: List<MemoryRelationshipEntity> = emptyList()
    ): List<WeightedEdge> {
        if (nodes.size < 2) return emptyList()

        val manualEdgeMap = mutableMapOf<Pair<Long, Long>, MemoryRelationshipEntity>()
        for (rel in existingManualEdges) {
            val key = if (rel.sourceMemoryId < rel.targetMemoryId) {
                rel.sourceMemoryId to rel.targetMemoryId
            } else {
                rel.targetMemoryId to rel.sourceMemoryId
            }
            manualEdgeMap[key] = rel
        }

        val allCandidateEdges = mutableListOf<WeightedEdge>()

        // For each node, find top candidates (k-NN) to avoid unscaled O(N^2) edge generation
        for (i in 0 until nodes.size) {
            val nodeA = nodes[i]
            val candidateEdgesForNode = mutableListOf<WeightedEdge>()

            for (j in (i + 1) until nodes.size) {
                val nodeB = nodes[j]

                // Check manual overrides first
                val key = if (nodeA.memory.id < nodeB.memory.id) {
                    nodeA.memory.id to nodeB.memory.id
                } else {
                    nodeB.memory.id to nodeA.memory.id
                }

                val manualRel = manualEdgeMap[key]
                if (manualRel != null && manualRel.relationshipType == RelationshipType.USER_REJECTED) {
                    // User explicitly unlinked these memories
                    continue
                }

                val edge = calculateEdge(nodeA, nodeB, manualRel)
                if (edge != null && (edge.totalWeight >= weights.minEdgeThreshold || edge.type == RelationshipType.USER_CONFIRMED)) {
                    candidateEdgesForNode.add(edge)
                }
            }

            // Keep top-K neighbors for nodeA to retain high modularity and graph sparsity
            candidateEdgesForNode.sortByDescending { it.totalWeight }
            val topK = candidateEdgesForNode.take(weights.maxNeighborsPerNode)
            allCandidateEdges.addAll(topK)
        }

        // Deduplicate undirected edges
        val edgeMap = mutableMapOf<Pair<Long, Long>, WeightedEdge>()
        for (edge in allCandidateEdges) {
            val key = if (edge.sourceId < edge.targetId) edge.sourceId to edge.targetId else edge.targetId to edge.sourceId
            val existing = edgeMap[key]
            if (existing == null || edge.totalWeight > existing.totalWeight) {
                edgeMap[key] = edge
            }
        }

        return edgeMap.values.toList()
    }

    /**
     * Calculates the normalized multi-signal similarity between two memories.
     */
    fun calculateEdge(
        nodeA: MemoryGraphNode,
        nodeB: MemoryGraphNode,
        manualRel: MemoryRelationshipEntity? = null
    ): WeightedEdge? {
        if (manualRel != null && manualRel.relationshipType == RelationshipType.USER_CONFIRMED) {
            return WeightedEdge(
                sourceId = nodeA.memory.id,
                targetId = nodeB.memory.id,
                totalWeight = 1.0f,
                semanticSim = manualRel.semanticSimilarity,
                temporalSim = manualRel.temporalSimilarity,
                geoSim = manualRel.geoSimilarity,
                keywordSim = manualRel.keywordSimilarity,
                type = RelationshipType.USER_CONFIRMED
            )
        }

        var activeWeightSum = 0f
        var totalWeightedSim = 0f

        // 1. Semantic embedding similarity
        var semanticSim = 0f
        if (nodeA.embedding != null && nodeB.embedding != null) {
            val vecA = embeddingEngine.unpackVector(nodeA.embedding.embeddingVector)
            val vecB = embeddingEngine.unpackVector(nodeB.embedding.embeddingVector)
            semanticSim = embeddingEngine.cosineSimilarity(vecA, vecB).coerceIn(0f, 1f)
            totalWeightedSim += semanticSim * weights.semanticWeight
            activeWeightSum += weights.semanticWeight
        }

        // 2. Keyword Jaccard similarity
        var keywordSim = 0f
        if (nodeA.keywords.isNotEmpty() && nodeB.keywords.isNotEmpty()) {
            val intersection = nodeA.keywords.intersect(nodeB.keywords).size
            val union = nodeA.keywords.union(nodeB.keywords).size
            if (union > 0) {
                keywordSim = (intersection.toFloat() / union.toFloat()).coerceIn(0f, 1f)
                totalWeightedSim += keywordSim * weights.keywordWeight
                activeWeightSum += weights.keywordWeight
            }
        }

        // 3. Temporal proximity (Gaussian decay: high for same day, drops smoothly over weeks)
        var temporalSim = 0f
        val timeA = nodeA.metadata?.dateTaken ?: nodeA.memory.createdAt
        val timeB = nodeB.metadata?.dateTaken ?: nodeB.memory.createdAt
        if (timeA > 0 && timeB > 0) {
            val deltaHours = abs(timeA - timeB) / (1000.0 * 3600.0)
            // Sigma ~ 72 hours (3 days) for events, exponential decay tail for seasons
            temporalSim = exp(-0.5 * (deltaHours / 72.0).pow(2.0)).toFloat().coerceIn(0f, 1f)
            totalWeightedSim += temporalSim * weights.temporalWeight
            activeWeightSum += weights.temporalWeight
        }

        // 4. Geographic proximity (Haversine distance decay)
        var geoSim = 0f
        val latA = nodeA.metadata?.latitude
        val lonA = nodeA.metadata?.longitude
        val latB = nodeB.metadata?.latitude
        val lonB = nodeB.metadata?.longitude

        if (latA != null && lonA != null && latB != null && lonB != null) {
            val distanceKm = haversineDistanceKm(latA, lonA, latB, lonB)
            // Gaussian decay: within 2km is very high (>0.8), drops to near 0 past 25km
            geoSim = exp(-0.5 * (distanceKm / 5.0).pow(2.0)).toFloat().coerceIn(0f, 1f)
            totalWeightedSim += geoSim * weights.geographicWeight
            activeWeightSum += weights.geographicWeight
        }

        if (activeWeightSum <= 0.0001f) {
            return null
        }

        // Renormalize over available modalities so missing GPS/time is never penalised
        val finalNormalizedScore = (totalWeightedSim / activeWeightSum).coerceIn(0f, 1f)

        return WeightedEdge(
            sourceId = nodeA.memory.id,
            targetId = nodeB.memory.id,
            totalWeight = finalNormalizedScore,
            semanticSim = semanticSim,
            temporalSim = temporalSim,
            geoSim = geoSim,
            keywordSim = keywordSim,
            type = RelationshipType.AI_DISCOVERED
        )
    }

    /**
     * Haversine distance in kilometers between two GPS coordinates.
     */
    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
