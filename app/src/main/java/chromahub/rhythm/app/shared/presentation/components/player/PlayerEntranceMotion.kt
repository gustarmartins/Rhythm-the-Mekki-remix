/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.player

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

private val entrancePopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Read the frame clock in the layer phase; content/layout do not change during an entrance. */
internal fun Modifier.playerEntranceMotion(
    progress: State<Float>, start: Float, duration: Float, travel: Dp
): Modifier = graphicsLayer {
    val raw = ((progress.value - start) / duration).coerceIn(0f, 1f)
    val fraction = FastOutSlowInEasing.transform(raw)
    val pop = entrancePopEasing.transform(raw)
    val scale = 0.88f + 0.12f * pop.coerceIn(0f, 1f) + 0.45f * (pop - 1f).coerceAtLeast(0f)
    alpha = fraction
    translationY = travel.toPx() * (1f - fraction)
    scaleX = scale
    scaleY = scale
}
