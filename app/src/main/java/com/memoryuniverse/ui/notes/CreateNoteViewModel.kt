package com.memoryuniverse.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryuniverse.MemoryUniverseApp
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CreateNoteUiState(
    val title: String = "",
    val content: String = "",
    val selectedClusterId: Long? = 1L,
    val parentPhotoId: Long? = null,
    val parentPhotoTitle: String? = null,
    val isAttachedNote: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val clusters: List<ClusterEntity> = emptyList(),
    val errorMessage: String? = null
)

class CreateNoteViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoryUniverseApp
    private val noteRepository = app.noteRepository
    private val clusterDao = app.database.clusterDao()

    private val _uiState = MutableStateFlow(CreateNoteUiState())
    val uiState: StateFlow<CreateNoteUiState> = _uiState.asStateFlow()

    init {
        loadClusters()
    }

    private fun loadClusters() {
        viewModelScope.launch {
            clusterDao.getAllClustersFlow().collectLatest { clusterList ->
                _uiState.value = _uiState.value.copy(clusters = clusterList)
            }
        }
    }

    fun setParentPhoto(photoId: Long, photoTitle: String?) {
        _uiState.value = _uiState.value.copy(
            parentPhotoId = photoId,
            parentPhotoTitle = photoTitle,
            isAttachedNote = true,
            title = "Note on ${photoTitle ?: "Photo #$photoId"}"
        )
    }

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
    }

    fun onContentChange(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
    }

    fun onClusterSelected(clusterId: Long) {
        _uiState.value = _uiState.value.copy(selectedClusterId = clusterId)
    }

    fun saveNote(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.content.isBlank() && state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Note cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            try {
                if (state.isAttachedNote && state.parentPhotoId != null) {
                    noteRepository.createPhotoAttachedNote(
                        photoMemoryId = state.parentPhotoId,
                        text = state.content.ifBlank { state.title }
                    )
                } else {
                    noteRepository.createStandaloneNote(
                        title = state.title.ifBlank { "Untitled Thought" },
                        content = state.content,
                        clusterId = state.selectedClusterId
                    )
                }
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save note: ${e.message}"
                )
            }
        }
    }
}
