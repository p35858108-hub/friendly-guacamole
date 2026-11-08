package com.siri.assistant.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.siri.assistant.AssistantUiState
import com.siri.assistant.Phase
import com.siri.assistant.ResponseState

@Composable
fun SiriOverlay(
    state: AssistantUiState,
    onOrbClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceClick: () -> Unit,
    onDismissResponse: () -> Unit,
    onDismissAll: () -> Unit,
) {
    // Entry: the whole layer springs in from slightly scaled down, like iOS invoking Siri.
    val entry = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entry.animateTo(1f, IosSprings.morph()) }

    val glowIntensity = when {
        state.response is ResponseState.Loading -> 1f
        state.phase == Phase.Input -> 0.75f
        else -> 0.9f
    }

    Box(Modifier.fillMaxSize()) {
        // Scrim + edge glow. Tapping empty space closes the assistant.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = entry.value.coerceIn(0f, 1f) }
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.15f),
                        1f to Color.Black.copy(alpha = 0.55f),
                    )
                )
                .intelligenceGlow(intensity = glowIntensity)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Close assistant",
                    onClick = onDismissAll,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * entry.value
                    scaleX = s
                    scaleY = s
                    translationY = (1f - entry.value) * 80.dp.toPx()
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ResponseCard(response = state.response, onDismiss = onDismissResponse)

            MorphingInputBar(
                expanded = state.phase == Phase.Input,
                query = state.query,
                onExpand = onOrbClick,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                onVoiceClick = onVoiceClick,
            )
        }
    }
}
