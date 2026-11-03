package com.memoryuniverse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memoryuniverse.core.theme.AstralBlue
import com.memoryuniverse.core.theme.CosmicViolet
import com.memoryuniverse.core.theme.DarkNebula
import com.memoryuniverse.core.theme.DeepVoid
import com.memoryuniverse.core.theme.GlassBorder
import com.memoryuniverse.core.theme.SpaceBlack
import com.memoryuniverse.core.theme.StarlightMuted
import com.memoryuniverse.core.theme.StarlightSecondary
import com.memoryuniverse.core.theme.StarlightWhite
import kotlin.random.Random

/**
 * Procedural deep space starfield rendered directly onto Compose Canvas.
 * Generates distant starlight particles and subtle nebular dust.
 */
@Composable
fun StarfieldBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "twinkle")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Stable random seed for star positions
    val stars = remember {
        val rand = Random(42)
        List(140) {
            Star(
                xNorm = rand.nextFloat(),
                yNorm = rand.nextFloat(),
                size = rand.nextFloat() * 2.2f + 0.8f,
                brightness = rand.nextFloat() * 0.7f + 0.3f,
                isTwinkling = rand.nextBoolean()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(SpaceBlack)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Deep space subtle nebula gradient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x187B61FF), // Faint violet nebula
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, height * 0.25f),
                    radius = width * 0.85f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x140284C7), // Faint astral blue nebula
                        Color.Transparent
                    ),
                    center = Offset(width * 0.2f, height * 0.8f),
                    radius = width * 0.7f
                )
            )

            // Star points
            for (star in stars) {
                val alpha = if (star.isTwinkling) {
                    (star.brightness * alphaAnim).coerceIn(0.15f, 1f)
                } else {
                    star.brightness
                }

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = star.size,
                    center = Offset(star.xNorm * width, star.yNorm * height)
                )
            }
        }
    }
}

private data class Star(
    val xNorm: Float,
    val yNorm: Float,
    val size: Float,
    val brightness: Float,
    val isTwinkling: Boolean
)

/**
 * Deep Space Glassmorphic Card Container
 */
@Composable
fun CosmicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    var boxModifier = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    DeepVoid.copy(alpha = 0.85f),
                    DarkNebula.copy(alpha = 0.95f)
                )
            )
        )
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        boxModifier = boxModifier.clickable { onClick() }
    }

    Box(modifier = boxModifier, content = content)
}

/**
 * Minimal Cosmic Metric Chip
 */
@Composable
fun CosmicMetricChip(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = CosmicViolet
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1F111827))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = StarlightWhite,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = StarlightMuted,
            fontSize = 11.sp
        )
    }
}
