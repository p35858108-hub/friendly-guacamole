package com.siri.assistant.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Continuous 0..360 rotation shared by all glow elements. */
@Composable
fun rememberGlowRotation(durationMs: Int = 4_000): State<Float> =
    rememberInfiniteTransition(label = "glowRotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing)),
        label = "rotation",
    )

/** Sweep gradient whose angle rotates around [center] without rotating the shape itself. */
fun rotatingSweepBrush(center: Offset, degrees: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        SweepGradientShader(center = center, colors = GlowColors).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setRotate(degrees, center.x, center.y) })
        }
}

private val GlowLayers = listOf(
    5f to 0.12f,   // wide, faint bloom
    2.5f to 0.35f, // mid halo
    1f to 1f,      // crisp core line
)

/**
 * Apple-Intelligence-style living border around the screen (or any shape).
 * Stacked strokes fake a bloom without RenderEffect, so it runs on every API level.
 *
 * @param intensity 0 = hidden, 1 = full. Changes are animated with a soft spring.
 */
fun Modifier.intelligenceGlow(
    intensity: Float,
    cornerRadius: Dp = 44.dp,
    strokeWidth: Dp = 5.dp,
): Modifier = composed {
    val rotation by rememberGlowRotation()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val animatedIntensity by animateFloatAsState(intensity, IosSprings.gentle(), label = "intensity")

    drawWithContent {
        drawContent()
        val alpha = animatedIntensity * pulse
        if (alpha <= 0.01f) return@drawWithContent

        val base = strokeWidth.toPx()
        val radius = cornerRadius.toPx()
        val brush = rotatingSweepBrush(center, rotation)

        GlowLayers.forEach { (widthFactor, layerAlpha) ->
            val width = base * widthFactor
            val inset = width / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(inset, inset),
                size = Size(size.width - width, size.height - width),
                cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
                style = Stroke(width),
                alpha = (alpha * layerAlpha).coerceIn(0f, 1f),
            )
        }
    }
}
