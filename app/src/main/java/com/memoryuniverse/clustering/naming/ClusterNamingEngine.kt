package com.memoryuniverse.clustering.naming

import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.database.entity.MemoryEntity
import com.memoryuniverse.core.database.entity.PhotoMetadataEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates human-meaningful galaxy/cluster titles using local signals:
 * - Aggregated high-frequency TF-IDF keywords
 * - OCR scene headlines
 * - Date/Season formatting (e.g. "Summer 2024", "October Weekend")
 * - Geolocation context if present
 * - Preserves user-assigned names unconditionally
 */
class ClusterNamingEngine {

    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val seasonYearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    fun generateClusterName(
        existingCluster: ClusterEntity?,
        memories: List<MemoryEntity>,
        metadataList: List<PhotoMetadataEntity>,
        keywordsList: List<List<String>>,
        ocrTexts: List<String>
    ): String {
        // Strict user lock check: Never overwrite a user-given name
        if (existingCluster != null && existingCluster.userRenamed) {
            return existingCluster.name
        }

        if (memories.isEmpty()) {
            return existingCluster?.name ?: "Cosmic Nebula"
        }

        // 1. Keyword frequency histogram
        val frequencyMap = mutableMapOf<String, Int>()
        for (kwList in keywordsList) {
            for (kw in kwList) {
                val clean = kw.trim().lowercase(Locale.getDefault())
                if (clean.length > 2) {
                    frequencyMap[clean] = (frequencyMap[clean] ?: 0) + 1
                }
            }
        }

        val topKeywords = frequencyMap.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) } }

        // 2. Date span analysis
        val timestamps = metadataList.mapNotNull { it.dateTaken }.filter { it > 0 }
            .ifEmpty { memories.map { it.createdAt } }

        val timeAnchor = if (timestamps.isNotEmpty()) {
            val minTime = timestamps.minOrNull() ?: 0L
            val maxTime = timestamps.maxOrNull() ?: 0L
            val date = Date(minTime)
            val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
            val year = seasonYearFormat.format(date)

            val diffDays = (maxTime - minTime) / (1000 * 3600 * 24)
            if (diffDays <= 3) {
                "$month $year"
            } else {
                year
            }
        } else {
            ""
        }

        // 3. Compose meaningful name
        return when {
            topKeywords.size >= 2 && timeAnchor.isNotEmpty() -> {
                "${topKeywords[0]} & ${topKeywords[1]} ($timeAnchor)"
            }
            topKeywords.isNotEmpty() && timeAnchor.isNotEmpty() -> {
                "${topKeywords[0]} Collection ($timeAnchor)"
            }
            topKeywords.isNotEmpty() -> {
                "${topKeywords.joinToString(" & ")} Galaxy"
            }
            timeAnchor.isNotEmpty() -> {
                "Memories of $timeAnchor"
            }
            ocrTexts.any { it.isNotBlank() } -> {
                val headline = ocrTexts.first { it.isNotBlank() }
                    .lines().firstOrNull()?.take(24)?.trim()
                if (!headline.isNullOrBlank()) "Archive: $headline" else "Starlight Cluster"
            }
            else -> {
                "Luminous Nebula"
            }
        }
    }

    /**
     * Assigns distinctive celestial colors based on cluster identity and dominant themes.
     */
    fun suggestClusterColor(clusterId: Long, name: String): String {
        val palette = listOf(
            "#4F46E5", // Deep Indigo
            "#06B6D4", // Cyan Azure
            "#10B981", // Emerald Aurora
            "#F59E0B", // Amber Nova
            "#EC4899", // Magenta Nebula
            "#8B5CF6", // Violet Pulsar
            "#3B82F6", // Celestial Blue
            "#14B8A6", // Teal Cosmos
            "#F97316"  // Solar Flare
        )
        val hash = (clusterId.hashCode() xor name.hashCode()).let { kotlin.math.abs(it) }
        return palette[hash % palette.size]
    }
}
