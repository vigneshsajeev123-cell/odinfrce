package com.memoryuniverse.ui.clusters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryuniverse.clustering.ClusterManager
import com.memoryuniverse.clustering.ClusteringStats
import com.memoryuniverse.core.database.MemoryDatabase
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClusterWithMemories(
    val cluster: ClusterEntity,
    val memories: List<MemoryEntity>
)

data class ClustersUiState(
    val isClustering: Boolean = false,
    val lastStats: ClusteringStats? = null,
    val selectedCluster: ClusterEntity? = null,
    val selectedClusterMemories: List<MemoryEntity> = emptyList(),
    val errorMessage: String? = null
)

class ClustersViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MemoryDatabase.getInstance(application)
    private val clusterManager = ClusterManager.create(application)
    private val clusterDao = database.clusterDao()
    private val memoryDao = database.memoryDao()

    val clustersFlow: StateFlow<List<ClusterEntity>> = clusterDao.getAllClustersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ClustersUiState())
    val uiState: StateFlow<ClustersUiState> = _uiState.asStateFlow()

    fun runClustering(forceFullRebuild: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClustering = true, errorMessage = null)
            try {
                val stats = clusterManager.runAutomaticClustering(forceFullRebuild)
                _uiState.value = _uiState.value.copy(
                    isClustering = false,
                    lastStats = stats
                )
                // Refresh selected cluster if any
                _uiState.value.selectedCluster?.let { selectCluster(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClustering = false,
                    errorMessage = e.localizedMessage ?: "Clustering failed"
                )
            }
        }
    }

    fun selectCluster(cluster: ClusterEntity?) {
        if (cluster == null) {
            _uiState.value = _uiState.value.copy(selectedCluster = null, selectedClusterMemories = emptyList())
            return
        }
        viewModelScope.launch {
            val memories = memoryDao.getMemoriesByClusterId(cluster.id)
            _uiState.value = _uiState.value.copy(
                selectedCluster = cluster,
                selectedClusterMemories = memories
            )
        }
    }

    fun renameCluster(clusterId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            clusterManager.renameCluster(clusterId, newName.trim())
            // Update selected cluster state
            val updated = clusterDao.getClusterById(clusterId)
            if (updated != null && _uiState.value.selectedCluster?.id == clusterId) {
                _uiState.value = _uiState.value.copy(selectedCluster = updated)
            }
        }
    }

    fun mergeClusters(targetClusterId: Long, sourceClusterId: Long) {
        viewModelScope.launch {
            clusterManager.mergeClusters(targetClusterId, sourceClusterId)
            selectCluster(null)
        }
    }

    fun splitCluster(sourceClusterId: Long, memoryIdsToSplit: List<Long>, newName: String?) {
        viewModelScope.launch {
            val newClusterId = clusterManager.splitCluster(sourceClusterId, memoryIdsToSplit, newName)
            val newCluster = clusterDao.getClusterById(newClusterId)
            selectCluster(newCluster)
        }
    }

    fun moveMemory(memoryId: Long, targetClusterId: Long?) {
        viewModelScope.launch {
            clusterManager.moveMemoryToCluster(memoryId, targetClusterId, lockAssignment = true)
            _uiState.value.selectedCluster?.let { selectCluster(it) }
        }
    }

    fun toggleLock(memoryId: Long, currentLocked: Boolean) {
        viewModelScope.launch {
            clusterManager.setMemoryLocked(memoryId, !currentLocked)
            _uiState.value.selectedCluster?.let { selectCluster(it) }
        }
    }

    fun excludeMemory(memoryId: Long) {
        viewModelScope.launch {
            clusterManager.excludeMemory(memoryId)
            _uiState.value.selectedCluster?.let { selectCluster(it) }
        }
    }
}
