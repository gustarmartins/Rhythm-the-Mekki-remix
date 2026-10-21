/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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
    Box(modifier.drawBehind {
        val fraction = progress()
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
