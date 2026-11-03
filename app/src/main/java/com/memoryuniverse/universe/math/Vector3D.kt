package com.memoryuniverse.universe.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance 3D Vector for Universe spatial calculations.
 */
data class Vector3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3D): Vector3D =
        Vector3D(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3D): Vector3D =
        Vector3D(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): Vector3D =
        Vector3D(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Float): Vector3D =
        if (scalar != 0f) Vector3D(x / scalar, y / scalar, z / scalar) else ZERO

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    fun normalize(): Vector3D {
        val len = length()
        return if (len > 0.00001f) this / len else ZERO
    }

    fun dot(other: Vector3D): Float =
        x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x
    )

    fun distanceTo(other: Vector3D): Float =
        (this - other).length()

    fun lerp(target: Vector3D, t: Float): Vector3D {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3D(
            x = x + (target.x - x) * clampedT,
            y = y + (target.y - y) * clampedT,
            z = z + (target.z - z) * clampedT
        )
    }

    companion object {
        val ZERO = Vector3D(0f, 0f, 0f)
        val UP = Vector3D(0f, 1f, 0f)
        val FORWARD = Vector3D(0f, 0f, -1f)
        val RIGHT = Vector3D(1f, 0f, 0f)
    }
}

/**
 * 3D Ray for Screen-to-Universe Raycasting and Tap Selection.
 */
data class Ray3D(
    val origin: Vector3D,
    val direction: Vector3D
) {
    /**
     * Tests intersection between ray and a sphere (representing a memory object in 3D).
     */
    fun intersectsSphere(center: Vector3D, radius: Float): Float? {
        val oc = origin - center
        val a = direction.dot(direction)
        val b = 2f * oc.dot(direction)
        val c = oc.dot(oc) - radius * radius
        val discriminant = b * b - 4f * a * c

        if (discriminant < 0) return null

        val dist = (-b - sqrt(discriminant)) / (2f * a)
        return if (dist > 0) dist else null
    }
}

/**
 * 4x4 Matrix for 3D View and Perspective Projection calculations.
 */
class Matrix4x4 {
    val m = FloatArray(16)

    init {
        identity()
    }

    fun identity() {
        for (i in 0..15) m[i] = 0f
        m[0] = 1f
        m[5] = 1f
        m[10] = 1f
        m[15] = 1f
    }

    companion object {
        fun lookAt(eye: Vector3D, target: Vector3D, up: Vector3D): Matrix4x4 {
            val forward = (target - eye).normalize()
            val right = forward.cross(up).normalize()
            val trueUp = right.cross(forward).normalize()

            val matrix = Matrix4x4()
            matrix.m[0] = right.x
            matrix.m[4] = right.y
            matrix.m[8] = right.z
            matrix.m[12] = -right.dot(eye)

            matrix.m[1] = trueUp.x
            matrix.m[5] = trueUp.y
            matrix.m[9] = trueUp.z
            matrix.m[13] = -trueUp.dot(eye)

            matrix.m[2] = -forward.x
            matrix.m[6] = -forward.y
            matrix.m[10] = -forward.z
            matrix.m[14] = forward.dot(eye)

            matrix.m[3] = 0f
            matrix.m[7] = 0f
            matrix.m[11] = 0f
            matrix.m[15] = 1f
            return matrix
        }

        fun perspective(fovYRadians: Float, aspect: Float, near: Float, far: Float): Matrix4x4 {
            val tanHalfFov = kotlin.math.tan(fovYRadians / 2f)
            val matrix = Matrix4x4()
            for (i in 0..15) matrix.m[i] = 0f

            matrix.m[0] = 1f / (aspect * tanHalfFov)
            matrix.m[5] = 1f / tanHalfFov
            matrix.m[10] = -(far + near) / (far - near)
            matrix.m[11] = -1f
            matrix.m[14] = -(2f * far * near) / (far - near)
            return matrix
        }
    }
}
