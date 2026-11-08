package com.siri.assistant.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val OrbSize = 64.dp
private val BarHeight = 58.dp

/**
 * One surface that morphs from a glowing orb (Dynamic-Island feel) into the
 * "Search or Ask" pill. Width/height ride an underdamped spring so the pill
 * overshoots slightly and settles, like iOS.
 */
@Composable
fun MorphingInputBar(
    expanded: Boolean,
    query: String,
    onExpand: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val transition = updateTransition(expanded, label = "morph")
        val width by transition.animateDp({ IosSprings.morph() }, label = "width") {
            if (it) maxWidth else OrbSize
        }
        val height by transition.animateDp({ IosSprings.morph() }, label = "height") {
            if (it) BarHeight else OrbSize
        }

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (pressed && !expanded) 0.9f else 1f,
            animationSpec = IosSprings.bouncy(),
            label = "press",
        )

        Surface(
            modifier = Modifier
                .size(width, height)
                .scale(pressScale)
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            shape = CircleShape,
            color = GlassDark.copy(alpha = 0.92f),
            shadowElevation = 12.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        enabled = !expanded,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Open assistant",
                        onClick = onExpand,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 90)) + scaleIn(IosSprings.morph(), initialScale = 0.85f))
                            .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.85f))
                            .using(SizeTransform(clip = false))
                    },
                    label = "barContent",
                ) { isExpanded ->
                    if (isExpanded) {
                        InputRow(query, onQueryChange, onSubmit, onVoiceClick)
                    } else {
                        SiriOrb(size = OrbSize - 14.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InputRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(180) // let the spring open before the keyboard pushes the layout
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SiriOrb(size = 26.dp)

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Search or Ask", color = SecondaryLabel, fontSize = 17.sp)
                    }
                    inner()
                }
            },
        )

        AnimatedContent(targetState = query.isBlank(), label = "trailingAction") { empty ->
            if (empty) {
                IconButton(onClick = onVoiceClick) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Speak", tint = Color.White)
                }
            } else {
                IconButton(onClick = onSubmit) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

/** Rotating Apple-Intelligence gradient orb with a soft halo. */
@Composable
fun SiriOrb(size: Dp, modifier: Modifier = Modifier) {
    val rotation by rememberGlowRotation(durationMs = 3_000)
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val brush = rotatingSweepBrush(center, rotation)
                drawCircle(brush, radius = this.size.minDimension / 2f, alpha = 0.35f)
                drawCircle(brush, radius = this.size.minDimension / 2.6f)
                drawCircle(Color.White.copy(alpha = 0.55f), radius = this.size.minDimension / 7f)
            }
            .clearAndSetSemantics { },
    )
}
