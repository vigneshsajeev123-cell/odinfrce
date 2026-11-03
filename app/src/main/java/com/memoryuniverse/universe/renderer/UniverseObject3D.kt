package com.memoryuniverse.universe.renderer

import com.memoryuniverse.core.database.entity.MemoryType
import com.memoryuniverse.universe.math.Vector3D

enum class ObjectLOD {
    GALAXY_POINT,   // Deep distance: rendered as glowing star node
    CARD_OVERVIEW,  // Mid distance: card shape with subtle glow and badge
    CARD_DETAIL     // Close distance: full textured photo quad with metadata
}

/**
 * 3D Node representing a floating Memory object in the Universe.
 */
data class UniverseObject3D(
    val memoryId: Long,
    val memoryType: MemoryType,
    val title: String?,
    val textContent: String? = null,
    val parentMemoryId: Long? = null,
    val timestamp: Long,
    val mediaUri: String?,
    val clusterId: Long?,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    var position: Vector3D,
    var scale: Float = 1.0f,
    var baseWidth: Float = 16f,
    var baseHeight: Float = 20f,
    val clusterColorHex: String = "#8B5CF6"
) {
    // Computed projection cache for screen space rendering & hit testing
    var screenX: Float = 0f
    var screenY: Float = 0f
    var projectedWidth: Float = 0f
    var projectedHeight: Float = 0f
    var depthDistance: Float = 0f
    var isVisibleOnScreen: Boolean = false
    var currentLOD: ObjectLOD = ObjectLOD.CARD_OVERVIEW

    /**
     * Determines Level of Detail based on distance to the camera eye.
     */
    fun updateLOD(cameraEye: Vector3D) {
        val dist = position.distanceTo(cameraEye)
        depthDistance = dist
        currentLOD = when {
            dist > 250f -> ObjectLOD.GALAXY_POINT
            dist > 75f -> ObjectLOD.CARD_OVERVIEW
            else -> ObjectLOD.CARD_DETAIL
        }
    }

    /**
     * Checks if a 2D touch point (tapX, tapY) hits this projected memory card.
     */
    fun containsScreenPoint(tapX: Float, tapY: Float): Boolean {
        if (!isVisibleOnScreen) return false
        val halfW = (projectedWidth / 2f).coerceAtLeast(24f)
        val halfH = (projectedHeight / 2f).coerceAtLeast(24f)
        return tapX in (screenX - halfW)..(screenX + halfW) &&
               tapY in (screenY - halfH)..(screenY + halfH)
    }
}
