package com.siri.assistant.ui

import com.siri.assistant.ResponseState

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(28.dp)

/**
 * Pops up from below with a bouncy spring. Drag down to dismiss; drag up is rubber-banded.
 * On release it either flies away or springs back using the fling velocity, like a UIKit sheet.
 */
@Composable
fun ResponseCard(
    response: ResponseState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the last visible content around so the exit animation still has something to draw.
    var lastVisible by remember { mutableStateOf<ResponseState?>(null) }
    if (response !is ResponseState.Hidden) lastVisible = response

    AnimatedVisibility(
        visible = response !is ResponseState.Hidden,
        modifier = modifier,
        enter = slideInVertically(IosSprings.bouncy()) { it } + fadeIn(tween(160)),
        exit = slideOutVertically(IosSprings.gentle()) { it } + fadeOut(tween(160)),
    ) {
        lastVisible?.let { DraggableCard(it, onDismiss) }
    }
}

@Composable
private fun DraggableCard(response: ResponseState, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 140.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val query = when (response) {
        is ResponseState.Loading -> response.query
        is ResponseState.Success -> response.query
        ResponseState.Hidden -> ""
    }
    LaunchedEffect(query) { offsetY.snapTo(0f) }

    val dragState = rememberDraggableState { delta ->
        val resistance = if (offsetY.value + delta < 0f) 0.25f else 1f
        scope.launch { offsetY.snapTo(offsetY.value + delta * resistance) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer {
                val progress = (offsetY.value / (dismissThreshold * 2)).coerceIn(0f, 1f)
                scaleX = 1f - progress * 0.06f
                scaleY = scaleX
                alpha = 1f - progress * 0.5f
            }
            .border(1.dp, Color.White.copy(alpha = 0.08f), CardShape)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (offsetY.value > dismissThreshold || velocity > 2_500f) {
                        offsetY.animateTo(offsetY.value + dismissThreshold * 3, IosSprings.gentle(), velocity)
                        onDismiss()
                    } else {
                        offsetY.animateTo(0f, IosSprings.bouncy(), velocity)
                    }
                },
            ),
        shape = CardShape,
        color = GlassDark.copy(alpha = 0.94f),
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 5.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SiriOrb(size = 20.dp)
                Text(query, color = SecondaryLabel, fontSize = 14.sp, maxLines = 2)
            }

            AnimatedContent(
                targetState = response,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                contentKey = { it::class },
                label = "cardBody",
            ) { state ->
                when (state) {
                    is ResponseState.Success -> Text(
                        text = state.text,
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    else -> ThinkingDots()
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(Color.White, CircleShape),
            )
        }
    }
}
