package com.memoryuniverse.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(
    tableName = "memory_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memory_id"], unique = true)
    ]
)
data class MemoryEmbeddingEntity(
    @PrimaryKey
    @ColumnInfo(name = "memory_id")
    val memoryId: Long,

    @ColumnInfo(name = "embedding_blob")
    val embeddingBlob: ByteArray, // Packed 512 float32 array (2048 bytes)

    @ColumnInfo(name = "model_name")
    val modelName: String = "mobileclip-s0-v1",

    @ColumnInfo(name = "dimension")
    val dimension: Int = 512,

    @ColumnInfo(name = "extracted_keywords")
    val extractedKeywords: String? = null, // Comma-separated tags

    @ColumnInfo(name = "ocr_text")
    val ocrText: String? = null, // Extracted on-device OCR text

    @ColumnInfo(name = "processed_at")
    val processedAt: Long = System.currentTimeMillis()
) {
    /**
     * Unpacks binary byte blob into FloatArray of embedding weights.
     */
    fun toFloatArray(): FloatArray {
        val floatBuffer = ByteBuffer.wrap(embeddingBlob)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()
        val result = FloatArray(dimension)
        floatBuffer.get(result)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MemoryEmbeddingEntity
        return memoryId == other.memoryId
    }

    override fun hashCode(): Int = memoryId.hashCode()

    companion object {
        /**
         * Packs 512 FloatArray into a binary blob for efficient SQLite storage.
         */
        fun fromFloatArray(
            memoryId: Long,
            vector: FloatArray,
            modelName: String = "mobileclip-s0-v1",
            extractedKeywords: String? = null,
            ocrText: String? = null
        ): MemoryEmbeddingEntity {
            val byteBuffer = ByteBuffer.allocate(vector.size * 4)
                .order(ByteOrder.LITTLE_ENDIAN)
            val floatBuffer = byteBuffer.asFloatBuffer()
            floatBuffer.put(vector)

            return MemoryEmbeddingEntity(
                memoryId = memoryId,
                embeddingBlob = byteBuffer.array(),
                modelName = modelName,
                dimension = vector.size,
                extractedKeywords = extractedKeywords,
                ocrText = ocrText
            )
        }
    }
}
