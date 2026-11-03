package com.memoryuniverse.universe.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.memoryuniverse.core.database.entity.ClusterEntity
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightWhite
import com.memoryuniverse.core.theme.SupernovaRose
import com.memoryuniverse.universe.camera.CameraState
import com.memoryuniverse.universe.math.Vector3D
import kotlin.math.sin

/**
 * High-performance 3D projection rendering engine rendered directly via Compose DrawScope.
 * Guarantees smooth 60 FPS performance, perspective depth, frustum culling, and LOD rendering.
 */
class UniverseCanvasRenderer(
    private val starfield: Starfield3D = Starfield3D()
) {

    /**
     * Renders the complete 3D Universe scene.
     */
    fun renderUniverse(
        drawScope: DrawScope,
        cameraState: CameraState,
        objects: List<UniverseObject3D>,
        clusters: List<ClusterEntity>,
        selectedMemoryId: Long?,
        timeSeconds: Float
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Draw pitch-black base void
        drawScope.drawRect(color = SpaceBlack)

        // 2. Render 3D Starfield particles
        renderStarfield(drawScope, cameraState, width, height, timeSeconds)

        // 3. Render Galaxy Clusters (centriods and nebula halos)
        renderClusterNebulae(drawScope, cameraState, clusters, width, height)

        // 4. Transform and Depth-Sort Universe Memory Objects
        val viewMatrix = cameraState.getViewMatrix()
        val projMatrix = cameraState.getProjectionMatrix()
        val cameraEye = cameraState.getEffectiveEye()

        // Project 3D objects and filter visible ones
        val visibleObjects = ArrayList<UniverseObject3D>(objects.size)
        for (obj in objects) {
            obj.updateLOD(cameraEye)
            if (projectObject(obj, viewMatrix.m, projMatrix.m, width, height)) {
                visibleObjects.add(obj)
            }
        }

        // Depth sort: back to front (painter's algorithm)
        visibleObjects.sortByDescending { it.depthDistance }

        // 5. Render Constellation / Galaxy Links between objects in same cluster
        renderConstellationLines(drawScope, visibleObjects)

        // 6. Render Floating 3D Memory Cards
        for (obj in visibleObjects) {
            val isSelected = obj.memoryId == selectedMemoryId
            when (obj.currentLOD) {
                ObjectLOD.GALAXY_POINT -> {
                    renderLODPoint(drawScope, obj, isSelected)
                }
                ObjectLOD.CARD_OVERVIEW -> {
                    renderLODOverview(drawScope, obj, isSelected)
                }
                ObjectLOD.CARD_DETAIL -> {
                    renderLODDetail(drawScope, obj, isSelected)
                }
            }
        }
    }

    private fun renderStarfield(
        drawScope: DrawScope,
        camera: CameraState,
        screenWidth: Float,
        screenHeight: Float,
        time: Float
    ) {
        val eye = camera.getEffectiveEye()
        val forward = (camera.getEffectiveTarget() - eye).normalize()
        val right = forward.cross(camera.up).normalize()
        val up = right.cross(forward).normalize()

        val halfW = screenWidth / 2f
        val halfH = screenHeight / 2f
        val fovFactor = (screenHeight / 2f) / kotlin.math.tan(camera.fovDegrees * kotlin.math.PI.toFloat() / 360f)

        for (star in starfield.stars) {
            // Relative vector from camera
            val rel = star.position - eye

            // Dot with camera forward axis (Z-depth in view space)
            val zDepth = rel.dot(forward)
            if (zDepth <= 5f) continue // Behind or too close to near plane

            val xCam = rel.dot(right)
            val yCam = rel.dot(up)

            // Perspective division
            val screenX = halfW + (xCam / zDepth) * fovFactor
            val screenY = halfH - (yCam / zDepth) * fovFactor

            if (screenX in -20f..(screenWidth + 20f) && screenY in -20f..(screenHeight + 20f)) {
                val twinkle = (0.5f + 0.5f * sin(time * star.twinkleSpeed + star.twinklePhase)).coerceIn(0.2f, 1f)
                val distanceAttenuation = (1200f / zDepth).coerceIn(0.3f, 1.4f)
                val radius = (star.baseRadius * distanceAttenuation).coerceIn(0.8f, 4.5f)

                drawScope.drawCircle(
                    color = star.color.copy(alpha = (star.color.alpha * twinkle * distanceAttenuation).coerceIn(0.1f, 1f)),
                    radius = radius,
                    center = Offset(screenX, screenY)
                )
            }
        }
    }

    private fun renderClusterNebulae(
        drawScope: DrawScope,
        camera: CameraState,
        clusters: List<ClusterEntity>,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val eye = camera.getEffectiveEye()
        val forward = (camera.getEffectiveTarget() - eye).normalize()
        val right = forward.cross(camera.up).normalize()
        val up = right.cross(forward).normalize()
        val halfW = screenWidth / 2f
        val halfH = screenHeight / 2f
        val fovFactor = (screenHeight / 2f) / kotlin.math.tan(camera.fovDegrees * kotlin.math.PI.toFloat() / 360f)

        for (cluster in clusters) {
            val centroid = Vector3D(cluster.centroidX, cluster.centroidY, cluster.centroidZ)
            val rel = centroid - eye
            val zDepth = rel.dot(forward)
            if (zDepth <= 10f) continue

            val xCam = rel.dot(right)
            val yCam = rel.dot(up)
            val screenX = halfW + (xCam / zDepth) * fovFactor
            val screenY = halfH - (yCam / zDepth) * fovFactor

            val projectedRadius = (cluster.radius / zDepth) * fovFactor
            if (projectedRadius > 10f && screenX in -projectedRadius..(screenWidth + projectedRadius)) {
                // Draw soft cosmic nebula cloud around galaxy
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmicViolet.copy(alpha = 0.18f),
                            AstralBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(screenX, screenY),
                        radius = projectedRadius * 1.5f
                    ),
                    radius = projectedRadius * 1.5f,
                    center = Offset(screenX, screenY)
                )
            }
        }
    }

    private fun renderConstellationLines(
        drawScope: DrawScope,
        objects: List<UniverseObject3D>
    ) {
        val clusterGroups = objects.groupBy { it.clusterId }
        for ((_, group) in clusterGroups) {
            if (group.size < 2) continue
            // Connect neighbouring memories within the galaxy
            for (i in 0 until group.size - 1) {
                val a = group[i]
                val b = group[i + 1]
                if (a.isVisibleOnScreen && b.isVisibleOnScreen && a.depthDistance < 350f) {
                    val alpha = (1f - (a.depthDistance / 350f)).coerceIn(0.05f, 0.4f)
                    drawScope.drawLine(
                        color = CosmicViolet.copy(alpha = alpha),
                        start = Offset(a.screenX, a.screenY),
                        end = Offset(b.screenX, b.screenY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }
            }
        }

        // Render Photo-Attached Note orbital tethers
        for (obj in objects) {
            if (obj.memoryType == com.memoryuniverse.core.database.entity.MemoryType.PHOTO_ATTACHED_NOTE && obj.parentMemoryId != null) {
                val parent = objects.find { it.memoryId == obj.parentMemoryId }
                if (parent != null && obj.isVisibleOnScreen && parent.isVisibleOnScreen) {
                    // Ethereal cyan/amber tether connecting attached note to parent photo
                    drawScope.drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                        start = Offset(parent.screenX, parent.screenY),
                        end = Offset(obj.screenX, obj.screenY),
                        strokeWidth = 2.0f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }
            }
        }
    }

    private fun renderLODPoint(
        drawScope: DrawScope,
        obj: UniverseObject3D,
        isSelected: Boolean
    ) {
        val radius = if (isSelected) 7f else 4f
        val color = if (isSelected) AstralBlue else CosmicViolet
        // Star node glow
        drawScope.drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = radius * 2.2f,
            center = Offset(obj.screenX, obj.screenY)
        )
        drawScope.drawCircle(
            color = color,
            radius = radius,
            center = Offset(obj.screenX, obj.screenY)
        )
    }

    private fun renderLODOverview(
        drawScope: DrawScope,
        obj: UniverseObject3D,
        isSelected: Boolean
    ) {
        val w = obj.projectedWidth
        val h = obj.projectedHeight
        val topLeft = Offset(obj.screenX - w / 2f, obj.screenY - h / 2f)

        // Card glass background
        drawScope.drawRoundRect(
            color = Color(0xCC0D0D18),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Cosmic border
        val borderColor = if (isSelected) AstralBlue else if (obj.isFavorite) SupernovaRose else CosmicViolet
        drawScope.drawRoundRect(
            color = borderColor.copy(alpha = if (isSelected) 0.9f else 0.5f),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = if (isSelected) 2f else 1.2f)
        )

        // Inner glowing core
        drawScope.drawCircle(
            color = borderColor.copy(alpha = 0.6f),
            radius = 3f,
            center = Offset(obj.screenX, obj.screenY)
        )
    }

    private fun renderLODDetail(
        drawScope: DrawScope,
        obj: UniverseObject3D,
        isSelected: Boolean
    ) {
        val w = obj.projectedWidth
        val h = obj.projectedHeight
        val topLeft = Offset(obj.screenX - w / 2f, obj.screenY - h / 2f)

        // Outer selection halo
        if (isSelected) {
            drawScope.drawRoundRect(
                color = AstralBlue.copy(alpha = 0.25f),
                topLeft = Offset(topLeft.x - 6f, topLeft.y - 6f),
                size = Size(w + 12f, h + 12f),
                cornerRadius = CornerRadius(14f, 14f)
            )
        }

        // Floating card body
        val isNote = obj.memoryType == com.memoryuniverse.core.database.entity.MemoryType.NOTE ||
                     obj.memoryType == com.memoryuniverse.core.database.entity.MemoryType.PHOTO_ATTACHED_NOTE

        val baseGradient = if (isNote) {
            listOf(Color(0xE62A1B0A), Color(0xF5140D05)) // Warm deep cosmic amber
        } else {
            listOf(Color(0xE6151528), Color(0xF5080812)) // Deep nebula obsidian
        }

        drawScope.drawRoundRect(
            brush = Brush.verticalGradient(
                colors = baseGradient,
                startY = topLeft.y,
                endY = topLeft.y + h
            ),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(10f, 10f)
        )

        if (isNote) {
            // Render simulated manuscript lines for text notes
            val lineMargin = 8f
            val startY = topLeft.y + 12f
            val lineSpacing = (h - 24f) / 4f
            for (i in 0..3) {
                val lineY = startY + i * lineSpacing
                val lineWidth = if (i == 3) (w - lineMargin * 2f) * 0.6f else (w - lineMargin * 2f)
                drawScope.drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                    start = Offset(topLeft.x + lineMargin, lineY),
                    end = Offset(topLeft.x + lineMargin + lineWidth, lineY),
                    strokeWidth = 1.5f
                )
            }
        } else {
            // Floating photo preview viewport inside the 3D card
            val photoMargin = 6f
            val photoHeight = h * 0.65f
            drawScope.drawRoundRect(
                color = Color(0x442A2A48),
                topLeft = Offset(topLeft.x + photoMargin, topLeft.y + photoMargin),
                size = Size(w - photoMargin * 2f, photoHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }

        // Favorite indicator dot
        if (obj.isFavorite) {
            drawScope.drawCircle(
                color = SupernovaRose,
                radius = 4f,
                center = Offset(topLeft.x + w - 12f, topLeft.y + 12f)
            )
        }

        // Card Border
        val borderColor = if (isSelected) {
            AstralBlue
        } else if (isNote) {
            Color(0xFFF59E0B).copy(alpha = 0.8f) // Stellar Amber border
        } else if (obj.isFavorite) {
            SupernovaRose.copy(alpha = 0.7f)
        } else {
            CosmicViolet.copy(alpha = 0.6f)
        }

        drawScope.drawRoundRect(
            color = borderColor,
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = if (isSelected) 2.5f else 1.5f)
        )
    }

    private fun projectObject(
        obj: UniverseObject3D,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        screenWidth: Float,
        screenHeight: Float
    ): Boolean {
        val px = obj.position.x
        val py = obj.position.y
        val pz = obj.position.z

        // Multiply by View Matrix
        val vx = viewMatrix[0] * px + viewMatrix[4] * py + viewMatrix[8] * pz + viewMatrix[12]
        val vy = viewMatrix[1] * px + viewMatrix[5] * py + viewMatrix[9] * pz + viewMatrix[13]
        val vz = viewMatrix[2] * px + viewMatrix[6] * py + viewMatrix[10] * pz + viewMatrix[14]
        val vw = viewMatrix[3] * px + viewMatrix[7] * py + viewMatrix[11] * pz + viewMatrix[15]

        // Multiply by Projection Matrix
        val clipX = projMatrix[0] * vx + projMatrix[4] * vy + projMatrix[8] * vz + projMatrix[12] * vw
        val clipY = projMatrix[1] * vx + projMatrix[5] * vy + projMatrix[9] * vz + projMatrix[13] * vw
        val clipZ = projMatrix[2] * vx + projMatrix[6] * vy + projMatrix[10] * vz + projMatrix[14] * vw
        val clipW = projMatrix[3] * vx + projMatrix[7] * vy + projMatrix[11] * vz + projMatrix[15] * vw

        // Near-plane frustum culling
        if (clipW <= 0.1f) {
            obj.isVisibleOnScreen = false
            return false
        }

        // Perspective divide
        val ndcX = clipX / clipW
        val ndcY = clipY / clipW

        // Map to Screen Coordinates
        val sx = (ndcX + 1f) * 0.5f * screenWidth
        val sy = (1f - ndcY) * 0.5f * screenHeight

        // Perspective scale for card width/height
        val scaleFactor = (screenHeight / 2f) / (clipW * kotlin.math.tan(60f * kotlin.math.PI.toFloat() / 360f))
        val pw = (obj.baseWidth * obj.scale * scaleFactor).coerceIn(12f, 260f)
        val ph = (obj.baseHeight * obj.scale * scaleFactor).coerceIn(16f, 320f)

        obj.screenX = sx
        obj.screenY = sy
        obj.projectedWidth = pw
        obj.projectedHeight = ph

        // Viewport bounds culling with margin
        val isVisible = sx in -pw..(screenWidth + pw) && sy in -ph..(screenHeight + ph)
        obj.isVisibleOnScreen = isVisible
        return isVisible
    }
}
