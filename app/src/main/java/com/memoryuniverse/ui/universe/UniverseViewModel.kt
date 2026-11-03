package com.memoryuniverse.ui.universe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryuniverse.MemoryUniverseApp
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryPositionEntity
import com.memoryuniverse.universe.camera.CameraState
import com.memoryuniverse.universe.math.Vector3D
import com.memoryuniverse.universe.renderer.UniverseObject3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UniverseUiState(
    val cameraState: CameraState = CameraState(),
    val objects: List<UniverseObject3D> = emptyList(),
    val clusters: List<ClusterEntity> = emptyList(),
    val selectedMemory: UniverseObject3D? = null,
    val isAnimatingCamera: Boolean = false,
    val showFavoritesOnly: Boolean = false,
    val selectedClusterId: Long? = null
)

class UniverseViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoryUniverseApp
    private val memoryDao = app.database.memoryDao()
    private val clusterDao = app.database.clusterDao()

    private val _uiState = MutableStateFlow(UniverseUiState())
    val uiState: StateFlow<UniverseUiState> = _uiState.asStateFlow()

    private val relationshipDao = app.database.relationshipDao()
    private val layoutEngine = com.memoryuniverse.universe.positioning.ForceDirectedUniverseLayout()

    init {
        loadUniverseData()
    }

    private fun loadUniverseData() {
        viewModelScope.launch {
            memoryDao.getAllMemoriesWithMetadataFlow().collectLatest { memoryList ->
                // Check if any memories lack 3D positions
                val unpositioned = memoryList.filter { it.posX == null }
                if (unpositioned.isNotEmpty()) {
                    realignUniverse()
                }

                val objects = memoryList.map { m ->
                    UniverseObject3D(
                        memoryId = m.id,
                        memoryType = m.memoryType,
                        title = m.title ?: m.displayName,
                        textContent = m.textContent,
                        parentMemoryId = m.parentMemoryId,
                        timestamp = m.timestamp,
                        mediaUri = m.mediaUri,
                        clusterId = m.clusterId,
                        isFavorite = m.isFavorite,
                        isPinned = m.isLocked,
                        position = Vector3D(m.posX ?: 0f, m.posY ?: 0f, m.posZ ?: 0f)
                    )
                }

                _uiState.value = _uiState.value.copy(objects = objects)
            }
        }

        viewModelScope.launch {
            clusterDao.getAllClustersFlow().collectLatest { clusterList ->
                _uiState.value = _uiState.value.copy(clusters = clusterList)
            }
        }
    }

    /**
     * Runs force-directed 3D layout simulation to calculate celestial positions.
     * Retains high inertia for already positioned memories to preserve stability.
     */
    fun realignUniverse() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val memories = memoryDao.getAllMemoriesList()
                if (memories.isEmpty()) return@withContext

                val clusters = clusterDao.getAllClusters()
                val existingPositions = memoryDao.getAllPositions().associateBy { it.memoryId }
                val relationships = relationshipDao.getAllRelationships().map { rel ->
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

                // Persist new 3D positions
                memoryDao.insertPositions(layoutResult.positions)

                // Update cluster centroids
                for (cluster in layoutResult.clusters) {
                    clusterDao.updateCentroid(
                        clusterId = cluster.id,
                        cx = cluster.centroidX,
                        cy = cluster.centroidY,
                        cz = cluster.centroidZ,
                        radius = cluster.radius
                    )
                }
            }
        }
    }

    fun flyToCluster(clusterId: Long) {
        val cluster = _uiState.value.clusters.firstOrNull { it.id == clusterId } ?: return
        flyCameraTo(cluster.centroidX, cluster.centroidY, 80f)
    }

    fun onPan(deltaX: Float, deltaY: Float) {
        val currentCam = _uiState.value.cameraState
        _uiState.value = _uiState.value.copy(
            cameraState = currentCam.copy(
                panX = currentCam.panX + deltaX,
                panY = currentCam.panY + deltaY
            )
        )
    }

    fun onZoom(newZoom: Float) {
        val currentCam = _uiState.value.cameraState
        _uiState.value = _uiState.value.copy(
            cameraState = currentCam.copy(zoomDistance = newZoom)
        )
    }

    fun onViewportSizeChanged(width: Float, height: Float) {
        if (width > 0f && height > 0f) {
            val currentCam = _uiState.value.cameraState
            _uiState.value = _uiState.value.copy(
                cameraState = currentCam.copy(
                    viewportWidth = width,
                    viewportHeight = height
                )
            )
        }
    }

    /**
     * Hit tests screen touch coordinate against projected 3D universe objects.
     */
    fun onScreenTap(tapX: Float, tapY: Float) {
        val objects = _uiState.value.objects
        // Find front-most intersecting object
        val hit = objects
            .filter { it.isVisibleOnScreen && it.containsScreenPoint(tapX, tapY) }
            .minByOrNull { it.depthDistance }

        if (hit != null) {
            _uiState.value = _uiState.value.copy(selectedMemory = hit)
            // Smoothly fly camera towards the tapped memory
            flyCameraTo(hit.position.x, hit.position.y, 45f)
        } else {
            // Dismiss selection if tapping empty space
            _uiState.value = _uiState.value.copy(selectedMemory = null)
        }
    }

    fun onDoubleTap(tapX: Float, tapY: Float) {
        val hit = _uiState.value.objects
            .filter { it.isVisibleOnScreen && it.containsScreenPoint(tapX, tapY) }
            .minByOrNull { it.depthDistance }

        if (hit != null) {
            flyCameraTo(hit.position.x, hit.position.y, 25f)
        } else {
            resetView()
        }
    }

    fun flyCameraTo(targetX: Float, targetY: Float, targetZoom: Float) {
        viewModelScope.launch {
            val startCam = _uiState.value.cameraState
            val frames = 25
            for (i in 1..frames) {
                val t = i.toFloat() / frames
                // Ease out cubic interpolation
                val easeT = 1f - kotlin.math.pow((1f - t).toDouble(), 3.0).toFloat()

                val interpolatedPanX = startCam.panX + (targetX - startCam.panX) * easeT
                val interpolatedPanY = startCam.panY + (targetY - startCam.panY) * easeT
                val interpolatedZoom = startCam.zoomDistance + (targetZoom - startCam.zoomDistance) * easeT

                _uiState.value = _uiState.value.copy(
                    cameraState = startCam.copy(
                        panX = interpolatedPanX,
                        panY = interpolatedPanY,
                        zoomDistance = interpolatedZoom
                    )
                )
                kotlinx.coroutines.delay(16)
            }
        }
    }

    fun resetView() {
        flyCameraTo(0f, 0f, 150f)
    }

    fun zoomIn() {
        val current = _uiState.value.cameraState.zoomDistance
        onZoom((current * 0.7f).coerceAtLeast(_uiState.value.cameraState.minZoom))
    }

    fun zoomOut() {
        val current = _uiState.value.cameraState.zoomDistance
        onZoom((current * 1.4f).coerceAtMost(_uiState.value.cameraState.maxZoom))
    }

    fun toggleFavoritesFilter() {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = !_uiState.value.showFavoritesOnly)
    }

    fun dismissSelectedMemory() {
        _uiState.value = _uiState.value.copy(selectedMemory = null)
    }

    /**
     * Persists manual object position changes into Room.
     */
    fun persistObjectPosition(memoryId: Long, newPos: Vector3D) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                memoryDao.insertPosition(
                    MemoryPositionEntity(
                        memoryId = memoryId,
                        posX = newPos.x,
                        posY = newPos.y,
                        posZ = newPos.z,
                        isPinned = true
                    )
                )
            }
        }
    }
}
