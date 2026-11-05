/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Read-only effect strength display. Audio gain must never depend on touch position. */
@Composable
internal fun ArcProgressIndicator(
    value: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackThickness: Dp = 4.dp,
    thumbSize: Dp = 16.dp,
    waveAmplitude: Dp = 2.dp,
    waveLength: Dp = 20.dp,
    startAngle: Float = 135f,
    sweepAngle: Float = 270f
) {
    val density = LocalDensity.current
    val trackThicknessPx = with(density) { trackThickness.toPx() }
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val waveAmplitudePx = with(density) { waveAmplitude.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }
    val thumbRadius = thumbSizePx / 2

    // Normalize value
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    val phaseShift = 0f

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val minDim = min(width, height)

        // Calculate arc geometry
        val arcDiameter = minDim - thumbSizePx * 2 - waveAmplitudePx * 2
        val arcRadius = arcDiameter / 2
        val arcCenter = Offset(width / 2, height / 2)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val activeSweep = sweepAngle * normalizedValue

            val thumbGapPx = thumbRadius + 6.dp.toPx() + trackThicknessPx / 2f
            val thumbGapDeg = if (arcRadius > 0f) {
                Math.toDegrees(thumbGapPx.toDouble() / arcRadius.toDouble()).toFloat()
            } else 0f
            val inactiveStartAngle = startAngle + activeSweep + thumbGapDeg
            val inactiveSweep = sweepAngle - activeSweep - thumbGapDeg

            if (inactiveSweep > 0f) {
                drawArc(
                    color = inactiveTrackColor,
                    startAngle = inactiveStartAngle,
                    sweepAngle = inactiveSweep,
                    useCenter = false,
                    style = Stroke(width = trackThicknessPx, cap = StrokeCap.Round),
                    topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
                    size = Size(arcDiameter, arcDiameter)
                )
            }

            if (activeSweep > 0) {
                val wavePath = Path()

                val steps = (activeSweep * 2).toInt().coerceAtLeast(10)
                val angleStep = activeSweep / steps

                var firstPointSet = false

                for (i in 0..steps) {
                    val currentAngleDeg = startAngle + (i * angleStep)
                    val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble())

                    val angleFromStartRad = Math.toRadians((i * angleStep).toDouble())
                    val distanceAlongArc = arcRadius * angleFromStartRad

                    val k = (2 * PI) / waveLengthPx
                    val h = if (enabled) waveAmplitudePx * sin(k * distanceAlongArc + phaseShift) else 0.0

                    val r = arcRadius + h

                    val x = arcCenter.x + r * cos(currentAngleRad)
                    val y = arcCenter.y + r * sin(currentAngleRad)

                    if (!firstPointSet) {
                        wavePath.moveTo(x.toFloat(), y.toFloat())
                        firstPointSet = true
                    } else {
                        wavePath.lineTo(x.toFloat(), y.toFloat())
                    }
                }

                drawPath(
                    path = wavePath,
                    color = activeTrackColor,
                    style = Stroke(width = trackThicknessPx, cap = StrokeCap.Round)
                )
            }

            val thumbAngleDeg = startAngle + activeSweep
            val thumbAngleRad = Math.toRadians(thumbAngleDeg.toDouble())
            val thumbX = arcCenter.x + arcRadius * cos(thumbAngleRad)
            val thumbY = arcCenter.y + arcRadius * sin(thumbAngleRad)

            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX.toFloat(), thumbY.toFloat())
            )
        }
    }
}
