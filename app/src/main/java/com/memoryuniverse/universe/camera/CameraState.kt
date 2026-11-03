package com.memoryuniverse.universe.camera

import com.memoryuniverse.universe.math.Matrix4x4
import com.memoryuniverse.universe.math.Ray3D
import com.memoryuniverse.universe.math.Vector3D
import kotlin.math.PI
import kotlin.math.tan

/**
 * Encapsulates the 3D Camera State in the Memory Universe.
 * Supports smooth 2D-style planar panning with genuine 3D perspective depth.
 */
data class CameraState(
    val eye: Vector3D = Vector3D(0f, 0f, 150f),
    val target: Vector3D = Vector3D(0f, 0f, 0f),
    val up: Vector3D = Vector3D(0f, 1f, 0f),
    val fovDegrees: Float = 60f,
    val zoomDistance: Float = 150f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val viewportWidth: Float = 1080f,
    val viewportHeight: Float = 1920f
) {
    val aspectRatio: Float
        get() = if (viewportHeight > 0f) viewportWidth / viewportHeight else 1f

    val minZoom: Float = 12f
    val maxZoom: Float = 1000f

    /**
     * Calculates the effective eye position combining pan and zoom distance.
     */
    fun getEffectiveEye(): Vector3D = Vector3D(
        x = panX,
        y = panY,
        z = zoomDistance
    )

    fun getEffectiveTarget(): Vector3D = Vector3D(
        x = panX,
        y = panY,
        z = 0f
    )

    /**
     * Unprojects a 2D screen coordinate (x, y) into a 3D ray in universe space.
     */
    fun screenPointToRay(screenX: Float, screenY: Float): Ray3D {
        val effectiveEye = getEffectiveEye()
        val effectiveTarget = getEffectiveTarget()

        // Normalize device coordinates [-1, 1]
        val ndcX = (2f * screenX / viewportWidth) - 1f
        val ndcY = 1f - (2f * screenY / viewportHeight)

        val forward = (effectiveTarget - effectiveEye).normalize()
        val right = forward.cross(up).normalize()
        val trueUp = right.cross(forward).normalize()

        val fovRad = (fovDegrees * PI / 180f).toFloat()
        val tanHalfFov = tan(fovRad / 2f)

        val aspect = aspectRatio
        val rayDir = (forward + (right * (ndcX * tanHalfFov * aspect)) + (trueUp * (ndcY * tanHalfFov))).normalize()

        return Ray3D(origin = effectiveEye, direction = rayDir)
    }

    /**
     * Computes the 3D View-Projection Matrix.
     */
    fun getViewMatrix(): Matrix4x4 =
        Matrix4x4.lookAt(getEffectiveEye(), getEffectiveTarget(), up)

    fun getProjectionMatrix(): Matrix4x4 =
        Matrix4x4.perspective(
            fovYRadians = (fovDegrees * PI / 180f).toFloat(),
            aspect = aspectRatio,
            near = 0.5f,
            far = 2500f
        )
}
