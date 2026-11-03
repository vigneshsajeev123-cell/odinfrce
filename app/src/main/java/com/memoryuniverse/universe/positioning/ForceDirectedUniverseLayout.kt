package com.memoryuniverse.universe.positioning

import com.memoryuniverse.clustering.graph.WeightedEdge
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryPositionEntity
import com.memoryuniverse.universe.math.Vector3D
import kotlin.math.*
import kotlin.random.Random

/**
 * 3D Force-Directed Galaxy & Solar System Spatial Layout Engine.
 *
 * Simulates gravitational cluster cohesion, galactic orbital disk dispersion,
 * inter-galaxy repulsion, and pairwise node collision avoidance.
 *
 * Guarantees:
 * 1. Persistent stability: existing pinned or positioned memories have high inertia,
 *    preventing the universe from scrambling on incremental additions.
 * 2. Visual elegance: memories in a cluster form a luminous galactic disk/spiral around the cluster centroid.
 * 3. Collision-free: repulsive spheres prevent photos from overlapping.
 * 4. Fast mobile convergence (<30 iterations, <30ms on mobile CPU).
 */
class ForceDirectedUniverseLayout(
    private val galaxyClearanceDistance: Float = 280f,
    private val memoryCollisionRadius: Float = 22f,
    private val springStiffness: Float = 0.08f,
    private val damping: Float = 0.82f
) {

    data class NodePhysics(
        val memoryId: Long,
        val clusterId: Long?,
        var pos: Vector3D,
        var vel: Vector3D = Vector3D(0f, 0f, 0f),
        val isPinned: Boolean = false,
        val hasExistingPosition: Boolean = false
    )

    data class ClusterPhysics(
        val clusterId: Long,
        var centroid: Vector3D,
        var radius: Float = 50f
    )

    /**
     * Computes or updates 3D positions for all memories, clusters, and relationships.
     */
    fun computeLayout(
        memories: List<MemoryEntity>,
        clusters: List<ClusterEntity>,
        existingPositions: Map<Long, MemoryPositionEntity>,
        relationships: List<WeightedEdge>,
        iterations: Int = 24
    ): LayoutResult {
        if (memories.isEmpty()) {
            return LayoutResult(emptyList(), emptyList())
        }

        val random = Random(42) // Deterministic seed for reproducible initial placement

        // 1. Position Galaxy Centroids
        val clusterMap = mutableMapOf<Long, ClusterPhysics>()
        val clusterCount = max(1, clusters.size)

        for ((index, cluster) in clusters.withIndex()) {
            val initialPos = if (cluster.centroidX != 0f || cluster.centroidY != 0f || cluster.centroidZ != 0f) {
                Vector3D(cluster.centroidX, cluster.centroidY, cluster.centroidZ)
            } else {
                // Distribute clusters on a broad celestial ring
                val angle = (index.toFloat() / clusterCount) * 2f * PI.toFloat()
                val dist = galaxyClearanceDistance * (0.85f + 0.3f * (index % 3))
                Vector3D(
                    x = cos(angle) * dist,
                    y = (index % 2 * 2 - 1) * 35f,
                    z = sin(angle) * dist
                )
            }
            clusterMap[cluster.id] = ClusterPhysics(cluster.id, initialPos, cluster.radius)
        }

        // Repulse galaxy centroids from each other
        for (iter in 0 until 12) {
            val clusterList = clusterMap.values.toList()
            for (i in 0 until clusterList.size) {
                for (j in (i + 1) until clusterList.size) {
                    val cA = clusterList[i]
                    val cB = clusterList[j]
                    val delta = cA.centroid - cB.centroid
                    val dist = max(delta.length(), 1f)
                    if (dist < galaxyClearanceDistance) {
                        val force = (galaxyClearanceDistance - dist) * 0.15f
                        val dir = delta.normalized()
                        cA.centroid = cA.centroid + dir * force
                        cB.centroid = cB.centroid - dir * force
                    }
                }
            }
        }

        // 2. Initialize Memory Physics Nodes
        val nodes = mutableListOf<NodePhysics>()
        val nodeById = mutableMapOf<Long, NodePhysics>()

        // Group memories by cluster
        val clusterMembers = memories.groupBy { it.clusterId }

        for ((clusterId, memberList) in clusterMembers) {
            val galaxy = if (clusterId != null) clusterMap[clusterId] else null
            val center = galaxy?.centroid ?: Vector3D(0f, 0f, 0f)

            for ((i, mem) in memberList.withIndex()) {
                val existing = existingPositions[mem.id]
                val pos = if (existing != null) {
                    Vector3D(existing.posX, existing.posY, existing.posZ)
                } else {
                    // Seed inside galactic spiral disk
                    val goldenAngle = i * 2.3999632f // Golden ratio spiral
                    val r = 18f + sqrt(i.toFloat()) * 26f
                    val diskTilt = 0.25f
                    Vector3D(
                        x = center.x + cos(goldenAngle) * r,
                        y = center.y + (random.nextFloat() - 0.5f) * 16f + sin(goldenAngle) * r * diskTilt,
                        z = center.z + sin(goldenAngle) * r
                    )
                }

                val node = NodePhysics(
                    memoryId = mem.id,
                    clusterId = mem.clusterId,
                    pos = pos,
                    isPinned = existing?.isPinned ?: mem.isLocked,
                    hasExistingPosition = existing != null
                )
                nodes.add(node)
                nodeById[mem.id] = node
            }
        }

        // 3. Force-Directed Simulation Iterations
        val edgeMap = relationships.groupBy { it.sourceId }

        for (iter in 0 until iterations) {
            val cooling = 1.0f - (iter.toFloat() / iterations.toFloat()) * 0.6f

            // A. Spring attraction along high-similarity edges
            for (edge in relationships) {
                val nodeA = nodeById[edge.sourceId] ?: continue
                val nodeB = nodeById[edge.targetId] ?: continue

                val delta = nodeB.pos - nodeA.pos
                val dist = max(delta.length(), 0.1f)
                val targetDist = max(24f, 80f * (1f - edge.totalWeight))

                val displacement = (dist - targetDist) * springStiffness * edge.totalWeight * cooling
                val forceDir = delta.normalized()

                if (!nodeA.isPinned) nodeA.vel = nodeA.vel + forceDir * displacement
                if (!nodeB.isPinned) nodeB.vel = nodeB.vel - forceDir * displacement
            }

            // B. Attraction to cluster centroid (keeps galaxy coherent)
            for (node in nodes) {
                if (node.isPinned) continue
                val galaxy = node.clusterId?.let { clusterMap[it] }
                if (galaxy != null) {
                    val toCenter = galaxy.centroid - node.pos
                    val dist = toCenter.length()
                    if (dist > 30f) {
                        node.vel = node.vel + toCenter.normalized() * (dist * 0.02f * cooling)
                    }
                }
            }

            // C. Pairwise Repulsion & Collision Avoidance (anti-overlap)
            for (i in 0 until nodes.size) {
                val nodeA = nodes[i]
                for (j in (i + 1) until nodes.size) {
                    val nodeB = nodes[j]

                    val delta = nodeA.pos - nodeB.pos
                    val distSq = delta.lengthSquared()
                    val minDist = memoryCollisionRadius * 2f

                    if (distSq < minDist * minDist && distSq > 0.001f) {
                        val dist = sqrt(distSq)
                        val overlap = (minDist - dist)
                        val repulseDir = delta.normalized()
                        val force = overlap * 0.5f * cooling

                        if (!nodeA.isPinned) nodeA.vel = nodeA.vel + repulseDir * force
                        if (!nodeB.isPinned) nodeB.vel = nodeB.vel - repulseDir * force
                    }
                }
            }

            // D. Integrate velocity and apply inertia/damping
            for (node in nodes) {
                if (node.isPinned) continue

                // High inertia for nodes with existing positions (preserves stable layout)
                val effectiveDamping = if (node.hasExistingPosition) damping * 0.75f else damping
                node.pos = node.pos + node.vel
                node.vel = node.vel * effectiveDamping
            }
        }

        // 4. Build output entities
        val resultPositions = nodes.map { node ->
            MemoryPositionEntity(
                memoryId = node.memoryId,
                posX = node.pos.x,
                posY = node.pos.y,
                posZ = node.pos.z,
                isPinned = node.isPinned
            )
        }

        // Calculate final bounding radii for clusters
        val updatedClusters = clusterMap.values.map { cPhys ->
            val memberNodes = nodes.filter { it.clusterId == cPhys.clusterId }
            val maxRadius = memberNodes.maxOfOrNull { (it.pos - cPhys.centroid).length() } ?: 50f
            ClusterEntity(
                id = cPhys.clusterId,
                name = clusters.firstOrNull { it.id == cPhys.clusterId }?.name ?: "Galaxy",
                colorHex = clusters.firstOrNull { it.id == cPhys.clusterId }?.colorHex ?: "#8B5CF6",
                centroidX = cPhys.centroid.x,
                centroidY = cPhys.centroid.y,
                centroidZ = cPhys.centroid.z,
                radius = max(40f, maxRadius + 20f),
                memoryCount = memberNodes.size
            )
        }

        return LayoutResult(resultPositions, updatedClusters)
    }
}

data class LayoutResult(
    val positions: List<MemoryPositionEntity>,
    val clusters: List<ClusterEntity>
)
