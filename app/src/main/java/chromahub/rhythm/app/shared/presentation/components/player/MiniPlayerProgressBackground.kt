/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/** Keep per-frame progress reads in drawing, without measuring the artwork and text again. */
@Composable
internal fun MiniPlayerProgressBackground(
    progress: () -> Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Observe the target here, so even the lower-frequency playback position updates
    // do not recompose the parent mini-player, its artwork, or either text label.
    val foreground by chromahub.rhythm.app.util.OptionalWorkController.foreground.collectAsState()
    val target = progress()
    val animatedProgress = if (foreground) {
        animateFloatAsState(
            targetValue = target,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "miniPlayerProgress"
        )
    } else rememberUpdatedState(target)
    Box(modifier.drawBehind {
        val fraction = animatedProgress.value
        if (fraction > 0f) {
            val width = size.width * fraction.coerceIn(0.001f, 1f)
            // Match the old fractional-width Box clipped to CircleShape, including small fills.
            val radius = minOf(width, size.height) / 2f
            drawRoundRect(
                color = color,
                size = Size(width, size.height),
                topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f, 0f),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    })
}
