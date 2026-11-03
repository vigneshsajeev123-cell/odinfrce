package com.memoryuniverse.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryuniverse.MemoryUniverseApp
import com.memoryuniverse.core.database.dao.MemoryWithMetadata
import com.memoryuniverse.core.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val hasPermission: Boolean = false,
    val isSyncing: Boolean = false,
    val totalCount: Int = 0,
    val photoCount: Int = 0,
    val noteCount: Int = 0,
    val clusterCount: Int = 0,
    val memories: List<MemoryWithMetadata> = emptyList(),
    val statusMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoryUniverseApp
    private val repository = app.photoRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkPermission()
        observeDatabase()
    }

    fun checkPermission() {
        val granted = PermissionManager.hasPhotoPermission(getApplication())
        _uiState.value = _uiState.value.copy(hasPermission = granted)
        if (granted) {
            syncPhotos()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasPermission = granted,
            statusMessage = if (granted) "Photo access granted. Scanning universe..." else "Photo access required to explore local memories."
        )
        if (granted) {
            syncPhotos()
        }
    }

    fun syncPhotos() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, statusMessage = "Discovering memories on device...")
            try {
                val newCount = repository.syncLocalPhotos()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    statusMessage = if (newCount > 0) "Synchronized $newCount new memories into the universe" else "Universe is up to date"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    statusMessage = "Sync completed with local media"
                )
            }
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            combine(
                repository.allMemories,
                repository.totalCount,
                repository.photoCount,
                repository.noteCount,
                repository.allClusters
            ) { memories, total, photos, notes, clusters ->
                _uiState.value.copy(
                    memories = memories,
                    totalCount = total,
                    photoCount = photos,
                    noteCount = notes,
                    clusterCount = clusters.size
                )
            }.collect { updatedState ->
                _uiState.value = updatedState
            }
        }
    }

    fun toggleFavorite(memoryId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(memoryId, current)
        }
    }

    fun deleteMemory(memoryId: Long) {
        viewModelScope.launch {
            repository.deleteMemory(memoryId)
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
