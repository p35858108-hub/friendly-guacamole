package com.siri.assistant.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Apple Intelligence palette, closed loop so the sweep gradient has no seam. */
val GlowColors = listOf(
    Color(0xFFBC82F3),
    Color(0xFFF5B9EA),
    Color(0xFF8D9FFF),
    Color(0xFFFF6778),
    Color(0xFFFFBA71),
    Color(0xFFC686FF),
    Color(0xFFBC82F3),
)

val GlassDark = Color(0xFF1C1C1E)
val SecondaryLabel = Color(0x99EBEBF5)

/** iOS-like springs: a little overshoot, settles quickly. */
object IosSprings {
    fun <T> morph() = spring<T>(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    fun <T> gentle() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
}

@Composable
fun SiriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            surface = GlassDark,
            onSurface = Color.White,
        ),
        content = content,
    )
}
