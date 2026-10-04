/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Number of samples used to approximate the eased edge ramp. The alpha is evaluated with a
 * smoothstep curve rather than a straight line, so the fade has no hard "knee" where it starts
 * or finishes and reads as a genuinely smooth dissolve instead of a banded ramp.
 */
private const val EDGE_RAMP_STEPS = 10

/**
 * Default multiplier stretching the edge ramp beyond [fadeWidth]. Higher values make the fade
 * reach further inward and ease out more gradually.
 */
private const val DEFAULT_EDGE_SOFTNESS = 2.5f

/** Eased alpha ramp, transparent at position 0 and fully opaque at position 1. */
private val FadeInStops: Array<Pair<Float, Color>> = Array(EDGE_RAMP_STEPS + 1) { i ->
    val t = i / EDGE_RAMP_STEPS.toFloat()
    val eased = t * t * (3f - 2f * t)
    t to Color.Black.copy(alpha = eased)
}

/** Eased alpha ramp, fully opaque at position 0 and transparent at position 1. */
private val FadeOutStops: Array<Pair<Float, Color>> = Array(EDGE_RAMP_STEPS + 1) { i ->
    val t = i / EDGE_RAMP_STEPS.toFloat()
    val eased = 1f - t * t * (3f - 2f * t)
    t to Color.Black.copy(alpha = eased)
}

/**
 * Blends the leading/trailing edges of a horizontally scrollable row into whatever sits
 * behind it, fading an edge only while that side still has hidden content.
 *
 * The gradient spans [softness] times [fadeWidth] so it eases out further inward, starting
 * at the row edge and reaching full content more gradually.
 *
 * Pass [lazyListState] for a `LazyRow`, or [scrollState] for `Modifier.horizontalScroll`.
 * [fadeWidth] should roughly match the row's gutter (padding + contentPadding + spacing).
 *
 * The scroll flags are read through [derivedStateOf] and the placement of the offscreen
 * layer is fixed for the lifetime of the modifier, so the effect neither recomposes on every
 * scrolled pixel nor re-allocates its layer when a fade starts or stops.
 */
fun Modifier.horizontalEdgeBlend(
    lazyListState: LazyListState? = null,
    scrollState: ScrollState? = null,
    fadeWidth: Dp = 20.dp,
    softness: Float = DEFAULT_EDGE_SOFTNESS
): Modifier = composed {
    val canScrollBackward by remember(lazyListState, scrollState) {
        derivedStateOf {
            lazyListState?.canScrollBackward ?: scrollState?.canScrollBackward ?: false
        }
    }
    val canScrollForward by remember(lazyListState, scrollState) {
        derivedStateOf {
            lazyListState?.canScrollForward ?: scrollState?.canScrollForward ?: false
        }
    }

    val startFade by animateDpAsState(
        targetValue = if (canScrollBackward) fadeWidth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "HorizontalEdgeBlendStart"
    )

    val endFade by animateDpAsState(
        targetValue = if (canScrollForward) fadeWidth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "HorizontalEdgeBlendEnd"
    )

    this
        .graphicsLayer {
            compositingStrategy = if (startFade > 0.dp || endFade > 0.dp) {
                CompositingStrategy.Offscreen
            } else {
                CompositingStrategy.Auto
            }
        }
        .drawWithContent {
            drawContent()

            if (startFade > 0.dp) {
                val width = startFade.toPx() * softness
                drawRect(
                    brush = Brush.horizontalGradient(
                        *FadeInStops,
                        startX = 0f,
                        endX = width
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

            if (endFade > 0.dp) {
                val width = endFade.toPx() * softness
                drawRect(
                    brush = Brush.horizontalGradient(
                        *FadeOutStops,
                        startX = size.width - width,
                        endX = size.width
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
