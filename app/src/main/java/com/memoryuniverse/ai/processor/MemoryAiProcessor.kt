package com.memoryuniverse.ai.processor

import android.content.Context
import android.net.Uri
import com.memoryuniverse.ai.embedding.MultimodalEmbeddingEngine
import com.memoryuniverse.ai.featureextraction.KeywordExtractor
import com.memoryuniverse.ai.ocr.LocalOcrExtractor
import com.memoryuniverse.core.database.dao.EmbeddingDao
import com.memoryuniverse.core.database.dao.MemoryDao
import com.memoryuniverse.core.database.entity.MemoryEmbeddingEntity
import com.memoryuniverse.core.database.entity.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AiProcessingStatus(
    val isRunning: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentItemTitle: String? = null
)

class MemoryAiProcessor(
    private val context: Context,
    private val memoryDao: MemoryDao,
    private val embeddingDao: EmbeddingDao,
    private val embeddingEngine: MultimodalEmbeddingEngine = MultimodalEmbeddingEngine(),
    private val ocrExtractor: LocalOcrExtractor = LocalOcrExtractor(context)
) {

    private val _status = MutableStateFlow(AiProcessingStatus())
    val status: StateFlow<AiProcessingStatus> = _status.asStateFlow()

    /**
     * Executes offline background processing for all unindexed memories.
     */
    suspend fun processUnindexedMemories(): Int = withContext(Dispatchers.IO) {
        val unprocessedIds = embeddingDao.getUnprocessedMemoryIds()
        if (unprocessedIds.isEmpty()) return@withContext 0

        _status.value = AiProcessingStatus(
            isRunning = true,
            processedCount = 0,
            totalCount = unprocessedIds.size
        )

        var count = 0
        for (id in unprocessedIds) {
            val memory = memoryDao.getMemoryById(id) ?: continue
            _status.value = _status.value.copy(
                processedCount = count,
                currentItemTitle = memory.title ?: "Memory #$id"
            )

            var ocrText: String? = null
            var textToEmbed = ""

            when (memory.memoryType) {
                MemoryType.PHOTO -> {
                    // Extract text from photo offline via ML Kit
                    if (memory.mediaUri != null) {
                        try {
                            val uri = Uri.parse(memory.mediaUri)
                            val ocrRes = ocrExtractor.extractTextFromUri(uri)
                            ocrText = ocrRes.fullText.ifBlank { null }
                        } catch (e: Exception) {
                            // Fallback gracefully
                        }
                    }
                    val combinedText = listOfNotNull(memory.title, ocrText).joinToString(" ")
                    textToEmbed = combinedText
                }
                MemoryType.NOTE, MemoryType.PHOTO_ATTACHED_NOTE -> {
                    val combinedText = listOfNotNull(memory.title, memory.textContent).joinToString(" ")
                    textToEmbed = combinedText
                }
            }

            // Extract keywords
            val keywordsString = KeywordExtractor.extractKeywordsString(textToEmbed)

            // Generate 512-dim Multimodal Embedding
            val vector = embeddingEngine.encodeText(textToEmbed)

            // Insert into SQLite database
            val embeddingEntity = MemoryEmbeddingEntity.fromFloatArray(
                memoryId = memory.id,
                vector = vector,
                modelName = "mobileclip-s0-v1",
                extractedKeywords = keywordsString,
                ocrText = ocrText
            )
            embeddingDao.insertEmbedding(embeddingEntity)
            count++
        }

        _status.value = AiProcessingStatus(
            isRunning = false,
            processedCount = count,
            totalCount = unprocessedIds.size
        )
        count
    }
}
