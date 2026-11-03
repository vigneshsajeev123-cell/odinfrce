package com.memoryuniverse.ui.photoviewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryuniverse.MemoryUniverseApp
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.PhotoMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoViewerUiState(
    val memory: MemoryEntity? = null,
    val metadata: PhotoMetadataEntity? = null,
    val attachedNotes: List<MemoryEntity> = emptyList(),
    val isFavorite: Boolean = false,
    val showExifSheet: Boolean = false,
    val showAddNoteDialog: Boolean = false,
    val newNoteText: String = "",
    val isLoading: Boolean = true
)

class PhotoViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoryUniverseApp
    private val memoryDao = app.database.memoryDao()
    private val noteRepository = app.noteRepository

    private val _uiState = MutableStateFlow(PhotoViewerUiState())
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

    private var currentMemoryId: Long = 0L

    fun loadMemory(memoryId: Long) {
        currentMemoryId = memoryId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val memory = memoryDao.getMemoryById(memoryId)
            val isFav = memory?.isFavorite ?: false
            _uiState.value = _uiState.value.copy(
                memory = memory,
                isFavorite = isFav,
                isLoading = false
            )

            // Observe attached notes in real-time
            noteRepository.getAttachedNotesForPhoto(memoryId).collectLatest { notes ->
                _uiState.value = _uiState.value.copy(attachedNotes = notes)
            }
        }
    }

    fun toggleFavorite() {
        val mem = _uiState.value.memory ?: return
        val newFav = !_uiState.value.isFavorite
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFavorite = newFav)
            withContext(Dispatchers.IO) {
                memoryDao.setFavorite(mem.id, newFav)
            }
        }
    }

    fun toggleExifSheet() {
        _uiState.value = _uiState.value.copy(showExifSheet = !_uiState.value.showExifSheet)
    }

    fun showAddNoteDialog() {
        _uiState.value = _uiState.value.copy(showAddNoteDialog = true, newNoteText = "")
    }

    fun dismissAddNoteDialog() {
        _uiState.value = _uiState.value.copy(showAddNoteDialog = false)
    }

    fun onNewNoteTextChange(text: String) {
        _uiState.value = _uiState.value.copy(newNoteText = text)
    }

    fun addAttachedNote() {
        val text = _uiState.value.newNoteText.trim()
        if (text.isEmpty() || currentMemoryId == 0L) return

        viewModelScope.launch {
            noteRepository.createPhotoAttachedNote(currentMemoryId, text)
            _uiState.value = _uiState.value.copy(showAddNoteDialog = false, newNoteText = "")
        }
    }

    fun deleteAttachedNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }
}
