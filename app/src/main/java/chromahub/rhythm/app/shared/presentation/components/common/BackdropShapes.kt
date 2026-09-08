/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeProvider
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes

import kotlin.random.Random

@Composable
fun BoxScope.SplashBackgroundOrbs(
    shapes: List<SplashBackdropShape>
) {
    shapes.forEachIndexed { index, shape ->
        SplashBackdropShapeItem(shape = shape, index = index)
    }
}

@Composable
fun BoxScope.SplashBackdropShapeItem(
    shape: SplashBackdropShape,
    index: Int
) {
    val transition = rememberInfiniteTransition(label = "splashBackdropShape_$index")
    val pulse by transition.animateFloat(
        initialValue = shape.pulseMin,
        targetValue = shape.pulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.pulseDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapePulse_$index"
    )
    val driftX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.driftXDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapeDriftX_$index"
    )
    val driftY by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.driftYDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapeDriftY_$index"
    )
    val rotation by transition.animateFloat(
        initialValue = -shape.rotationDegrees,
        targetValue = shape.rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.rotationDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapeRotation_$index"
    )

    Surface(
        modifier = Modifier
            .align(shape.alignment)
            .offset(x = shape.offsetX, y = shape.offsetY)
            .size(shape.size)
            .graphicsLayer {
                translationX = driftX * shape.driftXPx
                translationY = driftY * shape.driftYPx
                scaleX = pulse
                scaleY = pulse
                rotationZ = rotation
                alpha = shape.alpha
            },
        shape = shape.shape,
        color = shape.color
    ) {}
}

data class SplashBackdropShape(
    val shape: Shape,
    val alignment: Alignment,
    val offsetX: Dp,
    val offsetY: Dp,
    val size: Dp,
    val color: androidx.compose.ui.graphics.Color,
    val alpha: Float,
    val driftXPx: Float,
    val driftYPx: Float,
    val pulseMin: Float,
    val pulseMax: Float,
    val pulseDurationMs: Int,
    val driftXDurationMs: Int,
    val driftYDurationMs: Int,
    val rotationDegrees: Float,
    val rotationDurationMs: Int
)

private data class Quad(
    val count: Int,
    val sizeMin: Int,
    val sizeRange: Int,
    val rotMin: Int,
    val rotRange: Int,
    val pulseBaseMin: Float,
    val pulseBaseRange: Float
)

fun buildSplashBackdropShapes(
    seed: Int,
    shapeIds: List<String>,
    preset: String?,
    screenWidthDp: Int,
    screenHeightDp: Int,
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
    tertiaryColor: androidx.compose.ui.graphics.Color,
    neutralColor: androidx.compose.ui.graphics.Color,
    sizeScale: Float = 1f
): List<SplashBackdropShape> {
    val random = Random(seed)
    val colors = listOf(primaryColor, secondaryColor, tertiaryColor, neutralColor)
    val shapePool = if (shapeIds.isNotEmpty()) shapeIds else listOf("CIRCLE")
    val shortestSideDp = minOf(screenWidthDp, screenHeightDp)
    val isCompact = shortestSideDp < 380

    val (count, sizeMin, sizeRange, rotMin, rotRange, pulseBaseMin, pulseBaseRange) = when (preset) {
        "MODERN" -> listOf(5, 34, 24, 0, 8, 0.96f, 0.04f)
        "FRIENDLY" -> listOf(5, 38, 26, 6, 18, 0.88f, 0.12f)
        "PLAYFUL" -> listOf(5, 36, 28, 4, 20, 0.86f, 0.18f)
        "GEOMETRIC" -> listOf(4, 32, 22, 0, 6, 0.96f, 0.03f)
        "ORGANIC" -> listOf(5, 40, 28, 6, 22, 0.86f, 0.14f)
        else -> listOf(5, 36, 26, 8, 16, 0.86f, 0.1f)
    }.let { tpl ->
        val c = tpl[0] as Int
        val smin = tpl[1] as Int
        val srange = tpl[2] as Int
        val rmin = tpl[3] as Int
        val rrange = tpl[4] as Int
        val pmin = tpl[5] as Float
        val prange = tpl[6] as Float
        val adjustedCount = if (shortestSideDp < 340) 4 else c
        val adjustedSizeMin = when {
            shortestSideDp < 340 -> 28
            shortestSideDp < 400 -> 32
            else -> 34
        }
        val adjustedSizeRange = when {
            shortestSideDp < 340 -> 18
            shortestSideDp < 400 -> 20
            else -> 22
        }
        Quad(adjustedCount, adjustedSizeMin, adjustedSizeRange, rmin, rrange, pmin, prange)
    }

    val anchors = listOf(
        Pair(0.12f, 0.14f),
        Pair(0.88f, 0.16f),
        Pair(0.14f, 0.84f),
        Pair(0.86f, 0.86f),
        Pair(0.06f, 0.48f),
        Pair(0.94f, 0.52f),
        Pair(0.24f, 0.10f),
        Pair(0.76f, 0.10f),
        Pair(0.18f, 0.92f),
        Pair(0.82f, 0.90f)
    )

    val placed = mutableListOf<Pair<Pair<Float, Float>, Float>>()
    val results = mutableListOf<SplashBackdropShape>()
    val selectedAnchors = anchors.shuffled(random).take(count)
    val selectedShapeIds = shapePool.shuffled(random).take(count)

    val virtualScale = 420f * sizeScale
    val offsetLimitX = if (isCompact) 12 else 20
    val offsetLimitY = if (isCompact) 14 else 24
    val centerClearMin = if (isCompact) 0.24f else 0.28f
    val centerClearMax = 1f - centerClearMin

    // Reserve extra space for rotation, pulse and drift so shapes never overlap
    // mid-animation. Margins are physical dp normalized against a fixed
    // reference scale (420dp) so they stay constant regardless of sizeScale.
    val referenceScale = 420f
    val rotationExtentFactor = 1.5f
    val pulseFactor = 1.15f
    val driftNorm = (if (isCompact) 8f else 12f) / referenceScale
    val offsetNorm = maxOf(offsetLimitX, offsetLimitY) / referenceScale
    val minGapNorm = (if (isCompact) 8f else 14f) / referenceScale

    var attemptsTotal = 0
    var placedCount = 0
    var i = 0
    while (i < count && attemptsTotal < count * 60) {
        attemptsTotal++

        val anchor = selectedAnchors.getOrElse(i) { anchors[random.nextInt(anchors.size)] }
        val jitterScale = if (isCompact) 0.035f else 0.05f
        val biasX = (anchor.first + (random.nextFloat() - 0.5f) * jitterScale).coerceIn(0.06f, 0.94f)
        val biasY = (anchor.second + (random.nextFloat() - 0.5f) * jitterScale).coerceIn(0.06f, 0.94f)

        if (biasX in centerClearMin..centerClearMax && biasY in centerClearMin..centerClearMax) {
            continue
        }

        // Shrink until this slot is collision-free so shapes are never dropped.
        val baseSizeDp = (sizeMin + random.nextInt(0, sizeRange)) * sizeScale
        var placedHere = false
        var shrinkAttempts = 0
        while (!placedHere && shrinkAttempts < 4) {
            val sizeDp = (baseSizeDp * (1f - 0.15f * shrinkAttempts)).coerceAtLeast(26f * sizeScale).dp
            val sizeVal = sizeDp.value
            val radiusNorm = (sizeVal / virtualScale) / 2f
            val radiusEff = radiusNorm * rotationExtentFactor * pulseFactor + driftNorm + offsetNorm

            val px = biasX
            val py = biasY

            val safe = placed.all { (pos, r) ->
                val dx = pos.first - px
                val dy = pos.second - py
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                dist > r + radiusEff + minGapNorm
            }

            if (safe) {
                placed.add(Pair(Pair(px, py), radiusEff))

                val alignment = BiasAlignment(px * 2f - 1f, py * 2f - 1f)
                val offsetX = random.nextInt(-offsetLimitX, offsetLimitX).dp
                val offsetY = random.nextInt(-offsetLimitY, offsetLimitY).dp
                val driftXPx = random.nextInt(if (isCompact) 1 else 3, if (isCompact) 8 else 12).toFloat()
                val driftYPx = random.nextInt(if (isCompact) 1 else 3, if (isCompact) 8 else 12).toFloat()
                val pulseMin = pulseBaseMin + random.nextFloat() * pulseBaseRange
                val pulseMax = pulseMin + 0.05f + random.nextFloat() * 0.08f
                val pulseDurationMs = random.nextInt(4200, 10800)
                val driftXDurationMs = random.nextInt(6800, 14800)
                val driftYDurationMs = random.nextInt(7200, 15600)
                val rotationDegrees = (rotMin + random.nextInt(0, rotRange)).toFloat()
                val rotationDurationMs = random.nextInt(7600, 16200)
                val selectedShapeId = selectedShapeIds.getOrElse(i) { shapePool[random.nextInt(shapePool.size)] }

                results.add(
                    SplashBackdropShape(
                        shape = ExpressiveShapeProvider.getShapeById(selectedShapeId, ExpressiveShapes.ExtraLarge),
                        alignment = alignment,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        size = sizeDp,
                        color = colors[random.nextInt(colors.size)],
                        alpha = 0.86f - (placedCount * 0.06f),
                        driftXPx = driftXPx,
                        driftYPx = driftYPx,
                        pulseMin = pulseMin,
                        pulseMax = pulseMax,
                        pulseDurationMs = pulseDurationMs,
                        driftXDurationMs = driftXDurationMs,
                        driftYDurationMs = driftYDurationMs,
                        rotationDegrees = rotationDegrees,
                        rotationDurationMs = rotationDurationMs
                    )
                )

                placedCount++
                placedHere = true
                i++
            } else {
                shrinkAttempts++
            }
        }

        if (!placedHere && attemptsTotal % 4 == 0) {
            i++
        }
    }

    if (results.isEmpty()) {
        val selectedShapeId = shapePool[random.nextInt(shapePool.size)]
        results.add(
            SplashBackdropShape(
                shape = ExpressiveShapeProvider.getShapeById(selectedShapeId, ExpressiveShapes.ExtraLarge),
                alignment = Alignment.TopCenter,
                offsetX = 0.dp,
                offsetY = if (isCompact) 12.dp else 20.dp,
                size = (sizeMin.coerceAtLeast(28)).dp,
                color = colors[0],
                alpha = 0.8f,
                driftXPx = if (isCompact) 4f else 8f,
                driftYPx = if (isCompact) 4f else 8f,
                pulseMin = pulseBaseMin,
                pulseMax = pulseBaseMin + 0.06f,
                pulseDurationMs = 6000,
                driftXDurationMs = 10000,
                driftYDurationMs = 11000,
                rotationDegrees = rotMin.toFloat(),
                rotationDurationMs = 10000
            )
        )
    }

    return results
}
