package com.memoryuniverse.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memoryuniverse.MemoryUniverseApp
import com.memoryuniverse.ai.embedding.MultimodalEmbeddingEngine
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchResultItem(
    val memory: MemoryEntity,
    val similarityScore: Float, // 0.0 to 1.0
    val ocrSnippet: String? = null,
    val keywords: List<String> = emptyList()
)

enum class SearchFilter {
    ALL,
    PHOTOS,
    NOTES
}

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val filter: SearchFilter = SearchFilter.ALL,
    val results: List<SearchResultItem> = emptyList(),
    val totalIndexedAiMemories: Int = 0
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoryUniverseApp
    private val memoryDao = app.database.memoryDao()
    private val embeddingDao = app.database.embeddingDao()
    private val embeddingEngine = MultimodalEmbeddingEngine()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        triggerAiIndexing()
    }

    fun triggerAiIndexing() {
        viewModelScope.launch {
            app.memoryAiProcessor.processUnindexedMemories()
            val embeddings = embeddingDao.getAllEmbeddings()
            _uiState.value = _uiState.value.copy(totalIndexedAiMemories = embeddings.size)
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
        } else {
            performSemanticSearch(newQuery)
        }
    }

    fun onFilterChange(newFilter: SearchFilter) {
        _uiState.value = _uiState.value.copy(filter = newFilter)
        if (_uiState.value.query.isNotBlank()) {
            performSemanticSearch(_uiState.value.query)
        }
    }

    private fun performSemanticSearch(queryString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val searchResults = withContext(Dispatchers.IO) {
                // 1. Generate 512-dim embedding for user query
                val queryVector = embeddingEngine.encodeText(queryString)
                val allEmbeddings = embeddingDao.getAllEmbeddings()
                val activeFilter = _uiState.value.filter

                val scoredList = mutableListOf<SearchResultItem>()

                for (emb in allEmbeddings) {
                    val memory = memoryDao.getMemoryById(emb.memoryId) ?: continue

                    // Apply filter
                    if (activeFilter == SearchFilter.PHOTOS && memory.memoryType != MemoryType.PHOTO) continue
                    if (activeFilter == SearchFilter.NOTES && (memory.memoryType != MemoryType.NOTE && memory.memoryType != MemoryType.PHOTO_ATTACHED_NOTE)) continue

                    val itemVector = emb.toFloatArray()
                    var similarity = embeddingEngine.cosineSimilarity(queryVector, itemVector)

                    // Boost if exact query keyword is contained in title, OCR, or note content
                    val textContent = listOfNotNull(memory.title, memory.textContent, emb.ocrText).joinToString(" ").lowercase()
                    if (textContent.contains(queryString.lowercase().trim())) {
                        similarity = (similarity * 1.25f).coerceAtMost(1.0f)
                    }

                    if (similarity >= 0.15f) { // Threshold for relevance
                        scoredList.add(
                            SearchResultItem(
                                memory = memory,
                                similarityScore = similarity,
                                ocrSnippet = emb.ocrText?.take(80),
                                keywords = emb.extractedKeywords?.split(",")?.map { it.trim() } ?: emptyList()
                            )
                        )
                    }
                }

                scoredList.sortByDescending { it.similarityScore }
                scoredList
            }

            _uiState.value = _uiState.value.copy(
                results = searchResults,
                isSearching = false
            )
        }
    }
}
