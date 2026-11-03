package com.memoryuniverse.universe.renderer

import androidx.compose.ui.graphics.Color
import com.memoryuniverse.universe.math.Vector3D
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Star3D(
    val position: Vector3D,
    val baseRadius: Float,
    val color: Color,
    val twinkleSpeed: Float,
    var twinklePhase: Float
)

/**
 * Procedural 3D Starfield generator for the Pitch-Black Universe.
 * Generates thousands of stars across celestial shells with realistic spectral distributions.
 */
class Starfield3D(starCount: Int = 1800, seed: Long = 1337L) {

    val stars: List<Star3D>

    init {
        val rand = Random(seed)
        val generated = ArrayList<Star3D>(starCount)

        // Spectral class color palette
        val spectralColors = listOf(
            Color(0xFF93C5FD), // Class O/B: Hot Celestial Blue (20%)
            Color(0xFFF8FAFC), // Class A/F: Pure White Starlight (45%)
            Color(0xFFFDE047), // Class G: Solar Gold (15%)
            Color(0xFFFB923C), // Class K/M: Amber Giant (10%)
            Color(0xFFA855F7), // Cosmic Violet Dust (10%)
        )

        for (i in 0 until starCount) {
            // Fibonacci spherical distribution for natural astronomical randomness
            val u = rand.nextFloat()
            val v = rand.nextFloat()
            val theta = u * 2.0f * PI.toFloat()
            val phi = acos(2.0f * v - 1.0f)

            // Radius shells between 300 and 1800 units
            val radius = 300f + rand.nextFloat() * 1400f

            val sinPhi = sin(phi)
            val x = radius * sinPhi * cos(theta)
            val y = radius * sinPhi * sin(theta)
            val z = radius * cos(phi)

            val colorIndex = rand.nextInt(spectralColors.size)
            val starColor = spectralColors[colorIndex]
            val baseSize = when (colorIndex) {
                0 -> 1.8f + rand.nextFloat() * 1.4f
                1 -> 1.0f + rand.nextFloat() * 1.2f
                4 -> 2.2f + rand.nextFloat() * 1.5f
                else -> 1.2f + rand.nextFloat() * 1.0f
            }

            generated.add(
                Star3D(
                    position = Vector3D(x, y, z),
                    baseRadius = baseSize,
                    color = starColor,
                    twinkleSpeed = 0.8f + rand.nextFloat() * 2.5f,
                    twinklePhase = rand.nextFloat() * 2f * PI.toFloat()
                )
            )
        }

        stars = generated
    }
}
