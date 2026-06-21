package com.nexchat.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

// ─── Reusable Animation Specs ───────────────────────────────────────────────

object NexChatAnim {
    // Spring configs
    val bouncySpring = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)
    val smoothSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    val snappySpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)

    // Duration
    const val FAST = 150
    const val NORMAL = 250
    const val SLOW = 400
}

// ─── Modifier Extensions ────────────────────────────────────────────────────

/**
 * Animated press scale — use on any tappable composable.
 * Scales down on press, springs back on release.
 */
fun Modifier.animatePress(): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "press_scale"
    )
    this
        .scale(scale)
        .graphicsLayer {
            // Subtle elevation change on press
            shadowElevation = if (isPressed) 2f else 8f
        }
}

/**
 * Fade-in with slight upward slide.
 */
fun Modifier.animateEntry(): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(NexChatAnim.SLOW, easing = FastOutSlowInEasing),
        label = "entry_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "entry_offset"
    )
    this
        .alpha(alpha)
        .graphicsLayer { translationY = offsetY }
}

/**
 * Scale-in with bounce.
 */
fun Modifier.animatePopIn(): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "pop_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(NexChatAnim.FAST, easing = FastOutSlowInEasing),
        label = "pop_alpha"
    )
    this
        .scale(scale)
        .alpha(alpha)
}

/**
 * Shimmer loading effect placeholder.
 */
fun Modifier.shimmer(): Modifier = composed {
    this.background(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                androidx.compose.ui.graphics.Color(0xFF17171A),
                androidx.compose.ui.graphics.Color(0xFF1E1E22),
                androidx.compose.ui.graphics.Color(0xFF17171A)
            )
        )
    )
}
