/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Progress bar style options for MiniPlayer and Player
 * Following Compose December 2025 best practices with optimized animations
 */
enum class ProgressStyle {
    NORMAL,     // Standard LinearProgressIndicator
    WAVY,       // Animated wavy line
    ROUNDED,    // Rounded pill-shaped progress
    THIN,       // Thin elegant line
    THICK,      // Thick bold progress bar
    GRADIENT,   // Gradient colored progress
    SEGMENTED,  // Segmented/dotted progress
    DOTS        // Dots indicator
}

/**
 * Thumb style options for the progress bar slider
 */
enum class ThumbStyle(val shapeId: String?, val sizeScale: Float = 1f) {
    NONE(null),                     // No thumb
    DEFAULT(null),                  // Official M3 slider thumb composable
    CIRCLE("CIRCLE"),               // M3 Circle
    SQUARE("SQUARE"),               // M3 Square (rounded)
    PILL("PILL", 1.25f),            // M3 Pill
    DIAMOND("DIAMOND", 1.25f),      // M3 Diamond
    FLOWER("FLOWER", 1.25f),        // M3 Flower
    HEART("HEART", 1.25f),          // M3 Heart
    COOKIE("COOKIE_6", 1.25f),      // M3 Cookie 6-sided
    PUFFY("PUFFY", 1.25f);          // M3 Puffy

    companion object {
        /** Resolves a stored style name, mapping legacy names to the M3 set. */
        fun fromStorage(value: String?): ThumbStyle = when (value) {
            "NONE" -> NONE
            "DEFAULT", "GLOW", "ARROW" -> DEFAULT
            "OUTLINE", "DOT", "RING" -> CIRCLE
            "CIRCLE" -> CIRCLE
            "SQUARE" -> SQUARE
            "PILL", "LINE" -> PILL
            "DIAMOND" -> DIAMOND
            "FLOWER" -> FLOWER
            "HEART" -> HEART
            "COOKIE" -> COOKIE
            "PUFFY" -> PUFFY
            else -> DEFAULT
        }
    }
}

/**
 * Material 3 thumb for the progress bar slider.
 * [ThumbStyle.DEFAULT] renders the official M3 slider thumb; the rest render
 * Rhythm's M3 Expressive shapes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3Thumb(
    style: ThumbStyle,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    rotateWhenPlaying: Boolean = false
) {
    val effectiveSize = size * style.sizeScale

    // Slow spin while playing (render thread only)
    val rotation: Float = if (rotateWhenPlaying && isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "thumbRotate")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "thumbRotation"
        ).value
    } else {
        0f
    }
    val rotatedModifier = modifier.graphicsLayer { rotationZ = rotation }

    when (style) {
        ThumbStyle.NONE -> Unit
        ThumbStyle.DEFAULT -> {
            SliderDefaults.Thumb(
                interactionSource = remember { MutableInteractionSource() },
                modifier = rotatedModifier.size(effectiveSize),
                colors = SliderDefaults.colors(thumbColor = color),
                enabled = true,
                thumbSize = DpSize(effectiveSize, effectiveSize)
            )
        }
        else -> {
            // M3 Expressive shape via Rhythm's shape system. No elevation shadow
            // (path shadows on custom shapes are expensive and cause ANRs).
            val shape = remember(style) {
                ExpressiveShapeProvider.getShapeById(style.shapeId ?: "CIRCLE", CircleShape)
            }
            val thumbColor = SliderDefaults.colors(thumbColor = color).thumbColor
            Box(
                modifier = rotatedModifier
                    .size(effectiveSize)
                    .background(thumbColor, shape)
            )
        }
    }
}

/**
 * Unified progress bar composable that renders different styles
 */
@Composable
fun StyledProgressBar(
    progress: Float,
    style: ProgressStyle,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    height: Dp = 4.dp,
    isPlaying: Boolean = true,
    animated: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.DEFAULT,
    thumbSize: Dp = 12.dp,
    rotateThumbWhenPlaying: Boolean = false,
    waveAmplitudeWhenPlaying: Dp = 3.dp,
    waveLength: Dp = 40.dp
) {
    when (style) {
        ProgressStyle.NORMAL -> NormalProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height,
            isPlaying = isPlaying,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize,
            rotateThumbWhenPlaying = rotateThumbWhenPlaying
        )
        ProgressStyle.WAVY -> WavyProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height,
            isPlaying = isPlaying && animated,
            waveAmplitudeWhenPlaying = waveAmplitudeWhenPlaying,
            waveLength = waveLength
        )
        ProgressStyle.ROUNDED -> RoundedProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height,
            isPlaying = isPlaying,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize,
            rotateThumbWhenPlaying = rotateThumbWhenPlaying
        )
        ProgressStyle.THIN -> ThinProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            isPlaying = isPlaying,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize,
            rotateThumbWhenPlaying = rotateThumbWhenPlaying
        )
        ProgressStyle.THICK -> ThickProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            isPlaying = isPlaying,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize,
            rotateThumbWhenPlaying = rotateThumbWhenPlaying
        )
        ProgressStyle.GRADIENT -> GradientProgressBar(
            progress = progress,
            modifier = modifier,
            trackColor = trackColor,
            height = height,
            isPlaying = isPlaying,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize,
            rotateThumbWhenPlaying = rotateThumbWhenPlaying
        )
        ProgressStyle.SEGMENTED -> SegmentedProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height
        )
        ProgressStyle.DOTS -> DotsProgressBar(
            progress = progress,
            modifier = modifier,
            activeColor = progressColor,
            inactiveColor = trackColor
        )
    }
}

/**
 * Standard Material3 LinearProgressIndicator
 */
@Composable
private fun NormalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp,
    isPlaying: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.DEFAULT,
    thumbSize: Dp = 12.dp,
    rotateThumbWhenPlaying: Boolean = false
) {
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        val effectiveThumbSize = thumbSize * thumbStyle.sizeScale
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(height.coerceAtLeast(effectiveThumbSize))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val progressWidth = size.width * progress.coerceIn(0f, 1f)
                val centerY = size.height / 2
                val trackHeight = height.toPx()
                
                // Draw track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
                
                // Draw progress
                if (progressWidth > 0) {
                    drawRoundRect(
                        color = progressColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = androidx.compose.ui.geometry.Size(progressWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2)
                    )
                }
            }
            
            if (progress > 0f) {
                val thumbCenterX = (maxWidth * progress.coerceIn(0f, 1f))
                    .coerceIn(effectiveThumbSize / 2, maxWidth - effectiveThumbSize / 2)
                M3Thumb(
                    style = thumbStyle,
                    color = progressColor,
                    size = thumbSize,
                    isPlaying = isPlaying,
                    rotateWhenPlaying = rotateThumbWhenPlaying,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbCenterX - effectiveThumbSize / 2)
                )
            }
        }
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            color = progressColor,
            trackColor = trackColor
        )
    }
}

/**
 * Wavy animated progress bar - playful and musical
 * Enhanced with smooth amplitude transitions and bezier curve smoothing
 */
@Composable
private fun WavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp,
    isPlaying: Boolean,
    waveAmplitudeWhenPlaying: Dp = 3.dp,
    waveLength: Dp = 40.dp
) {
    // Smooth wave amplitude animation - only show wave when playing
    val animatedAmplitude by animateDpAsState(
        targetValue = if (isPlaying) waveAmplitudeWhenPlaying else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "WaveAmplitudeAnim"
    )
    
    // Conditional phase animation - only when wave should show
    val phaseShiftAnim = remember { Animatable(0f) }
    val phaseShift = phaseShiftAnim.value
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val fullRotation = (2 * PI).toFloat()
            while (isPlaying) {
                val start = (phaseShiftAnim.value % fullRotation).let { 
                    if (it < 0f) it + fullRotation else it 
                }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.coerceAtLeast(8.dp))
    ) {
        val width = size.width
        val centerY = size.height / 2
        val progressWidth = width * progress.coerceIn(0f, 1f)
        val waveAmplitude = animatedAmplitude.toPx().coerceAtLeast(0f)
        val strokeWidth = (size.height / 2).coerceIn(2f, 6f)
        
        // Draw track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Draw wavy progress
        if (progressWidth > 0) {
            if (waveAmplitude > 0.01f) {
                // Draw wavy line
                val path = Path()
                val waveLengthPx = waveLength.toPx()
                val waveFrequency = if (waveLengthPx > 0f) {
                    ((2 * PI) / waveLengthPx).toFloat()
                } else {
                    0f
                }
                
                val waveStartDrawX = 0f
                val waveEndDrawX = progressWidth.coerceAtLeast(waveStartDrawX)
                
                if (waveEndDrawX > waveStartDrawX) {
                    val periodPx = ((2 * PI) / waveFrequency).toFloat()
                    val samplesPerCycle = 20f
                    val waveStep = (periodPx / samplesPerCycle).coerceAtLeast(1.2f).coerceAtMost(strokeWidth)

                    fun yAt(x: Float): Float {
                        val s = sin(waveFrequency * x + phaseShift)
                        return (centerY + waveAmplitude * s).coerceIn(
                            centerY - waveAmplitude - strokeWidth / 2f,
                            centerY + waveAmplitude + strokeWidth / 2f
                        )
                    }

                    var prevX = waveStartDrawX
                    var prevY = yAt(prevX)
                    path.moveTo(prevX, prevY)

                    var x = prevX + waveStep
                    while (x < waveEndDrawX) {
                        val y = yAt(x)
                        val midX = (prevX + x) * 0.5f
                        val midY = (prevY + y) * 0.5f
                        path.quadraticTo(prevX, prevY, midX, midY)
                        prevX = x
                        prevY = y
                        x += waveStep
                    }
                    val endY = yAt(waveEndDrawX)
                    path.quadraticTo(prevX, prevY, waveEndDrawX, endY)

                    drawPath(
                        path = path,
                        color = progressColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            miter = 1f
                        )
                    )
                }
            } else {
                // Draw straight line when paused
                drawLine(
                    color = progressColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressWidth, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Rounded pill-shaped progress bar
 */
@Composable
private fun RoundedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp,
    isPlaying: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.DEFAULT,
    thumbSize: Dp = 12.dp,
    rotateThumbWhenPlaying: Boolean = false
) {
    val actualHeight = height.coerceAtLeast(6.dp)
    
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        val effectiveThumbSize = thumbSize * thumbStyle.sizeScale
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight.coerceAtLeast(effectiveThumbSize))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val progressWidth = size.width * progress.coerceIn(0f, 1f)
                val centerY = size.height / 2
                val trackHeight = actualHeight.toPx()
                
                // Draw track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
                
                // Draw progress
                if (progressWidth > 0) {
                    drawRoundRect(
                        color = progressColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(progressWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2)
                    )
                }
            }
            
            if (progress > 0f) {
                val thumbCenterX = (maxWidth * progress.coerceIn(0f, 1f))
                    .coerceIn(effectiveThumbSize / 2, maxWidth - effectiveThumbSize / 2)
                M3Thumb(
                    style = thumbStyle,
                    color = progressColor,
                    size = thumbSize,
                    isPlaying = isPlaying,
                    rotateWhenPlaying = rotateThumbWhenPlaying,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbCenterX - effectiveThumbSize / 2)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(actualHeight)
                    .clip(RoundedCornerShape(50))
                    .background(progressColor)
            )
        }
    }
}

/**
 * Thin elegant progress line - 2dp height
 */
@Composable
private fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    isPlaying: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.DEFAULT,
    thumbSize: Dp = 10.dp,
    rotateThumbWhenPlaying: Boolean = false
) {
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        val effectiveThumbSize = thumbSize * thumbStyle.sizeScale
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(effectiveThumbSize)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val width = size.width
                val centerY = size.height / 2
                
                // Track
                drawLine(
                    color = trackColor,
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Progress
                val progressWidth = width * progress.coerceIn(0f, 1f)
                if (progressWidth > 0) {
                    drawLine(
                        color = progressColor,
                        start = Offset(0f, centerY),
                        end = Offset(progressWidth, centerY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            if (progress > 0f) {
                val thumbCenterX = (maxWidth * progress.coerceIn(0f, 1f))
                    .coerceIn(effectiveThumbSize / 2, maxWidth - effectiveThumbSize / 2)
                M3Thumb(
                    style = thumbStyle,
                    color = progressColor,
                    size = thumbSize,
                    isPlaying = isPlaying,
                    rotateWhenPlaying = rotateThumbWhenPlaying,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbCenterX - effectiveThumbSize / 2)
                )
            }
        }
    } else {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            val width = size.width
            val centerY = size.height / 2
            
            // Track
            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Progress
            val progressWidth = width * progress.coerceIn(0f, 1f)
            if (progressWidth > 0) {
                drawLine(
                    color = progressColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressWidth, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Thick bold progress bar - 8dp height
 */
@Composable
private fun ThickProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    isPlaying: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.DEFAULT,
    thumbSize: Dp = 14.dp,
    rotateThumbWhenPlaying: Boolean = false
) {
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        val effectiveThumbSize = thumbSize * thumbStyle.sizeScale
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(8.dp.coerceAtLeast(effectiveThumbSize))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val progressWidth = size.width * progress.coerceIn(0f, 1f)
                val centerY = size.height / 2
                val trackHeight = 8.dp.toPx()
                
                // Draw track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                
                // Draw progress
                if (progressWidth > 0) {
                    drawRoundRect(
                        color = progressColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(progressWidth, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
            
            if (progress > 0f) {
                val thumbCenterX = (maxWidth * progress.coerceIn(0f, 1f))
                    .coerceIn(effectiveThumbSize / 2, maxWidth - effectiveThumbSize / 2)
                M3Thumb(
                    style = thumbStyle,
                    color = progressColor,
                    size = thumbSize,
                    isPlaying = isPlaying,
                    rotateWhenPlaying = rotateThumbWhenPlaying,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbCenterX - effectiveThumbSize / 2)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }
    }
}

/**
 * Gradient colored progress bar
 */
@Composable
private fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color,
    height: Dp,
    isPlaying: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.DEFAULT,
    thumbSize: Dp = 12.dp,
    rotateThumbWhenPlaying: Boolean = false
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    
    val actualHeight = height.coerceAtLeast(4.dp)
    
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        val effectiveThumbSize = thumbSize * thumbStyle.sizeScale
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight.coerceAtLeast(effectiveThumbSize))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val progressWidth = size.width * progress.coerceIn(0f, 1f)
                val centerY = size.height / 2
                val trackHeight = actualHeight.toPx()
                
                // Draw track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
                
                // Draw gradient progress
                if (progressWidth > 0) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(gradientColors),
                        topLeft = Offset(0f, centerY - trackHeight / 2),
                        size = Size(progressWidth, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2)
                    )
                }
            }
            
            if (progress > 0f) {
                val thumbCenterX = (maxWidth * progress.coerceIn(0f, 1f))
                    .coerceIn(effectiveThumbSize / 2, maxWidth - effectiveThumbSize / 2)
                M3Thumb(
                    style = thumbStyle,
                    color = gradientColors.last(),
                    size = thumbSize,
                    isPlaying = isPlaying,
                    rotateWhenPlaying = rotateThumbWhenPlaying,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbCenterX - effectiveThumbSize / 2)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(actualHeight)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(gradientColors)
                    )
            )
        }
    }
}

/**
 * Segmented progress bar with gaps
 */
@Composable
private fun SegmentedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp
) {
    val segments = 20
    val actualHeight = height.coerceAtLeast(4.dp)
    val filledSegments = (progress * segments).toInt()
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
    ) {
        val segmentWidth = (size.width - (segments - 1) * 3.dp.toPx()) / segments
        val cornerRadius = CornerRadius(size.height / 2)
        
        for (i in 0 until segments) {
            val x = i * (segmentWidth + 3.dp.toPx())
            val color = if (i < filledSegments) progressColor else trackColor
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = cornerRadius
            )
        }
    }
}

/**
 * Dots progress indicator
 */
@Composable
private fun DotsProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color
) {
    val dotCount = 12
    val activeDots = (progress * dotCount).toInt()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (i < activeDots) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * Compact mini progress bar for MiniPlayer - optimized for small spaces
 */
@Composable
fun MiniProgressBar(
    progress: Float,
    style: String,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    isPlaying: Boolean = true
) {
    val progressStyle = try {
        ProgressStyle.valueOf(style.uppercase())
    } catch (e: IllegalArgumentException) {
        ProgressStyle.NORMAL
    }
    
    StyledProgressBar(
        progress = progress,
        style = progressStyle,
        modifier = modifier,
        progressColor = progressColor,
        trackColor = trackColor,
        height = when (progressStyle) {
            ProgressStyle.THIN -> 2.dp
            ProgressStyle.THICK -> 6.dp
            ProgressStyle.WAVY -> 8.dp
            ProgressStyle.DOTS -> 6.dp
            ProgressStyle.SEGMENTED -> 4.dp
            else -> 4.dp
        },
        isPlaying = isPlaying,
        animated = true
    )
}

/**
 * Circular styled progress bar that wraps around content (like play/pause button)
 * Supports all progress styles including wavy, segmented, dots, etc.
 * The cornerRadius parameter allows the progress to adapt to button shape changes
 * (e.g., from circle when paused to rounded rect when playing)
 */
@Composable
fun CircularStyledProgressBar(
    progress: Float,
    style: ProgressStyle,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    strokeWidth: Dp = 3.dp,
    isPlaying: Boolean = true,
    cornerRadius: Dp = 50.dp, // 50.dp = circle, lower values = more rounded rect
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            ProgressStyle.WAVY -> WavyCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                isPlaying = isPlaying,
                cornerRadius = cornerRadius
            )
            ProgressStyle.SEGMENTED -> SegmentedCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.DOTS -> DottedCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.GRADIENT -> GradientCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.THIN -> ThinCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth * 0.6f,
                cornerRadius = cornerRadius
            )
            ProgressStyle.THICK -> ThickCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth * 1.5f,
                cornerRadius = cornerRadius
            )
            ProgressStyle.ROUNDED -> RoundedCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.NORMAL -> NormalCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
        }
        
        content()
    }
}

@Composable
private fun WavyCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    isPlaying: Boolean,
    cornerRadius: Dp = 50.dp
) {
    // Conditional phase animation - only when playing
    val phaseShiftAnim = remember { Animatable(0f) }
    val phaseShift = phaseShiftAnim.value
    
    // Wave amplitude animation - animates to 0 when paused (flat circle)
    val waveAmplitudeAnim by animateFloatAsState(
        targetValue = if (isPlaying) 0.3f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "waveAmplitude"
    )
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val fullRotation = (2 * PI).toFloat()
            while (isPlaying) {
                val start = (phaseShiftAnim.value % fullRotation).let { 
                    if (it < 0f) it + fullRotation else it 
                }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            // Draw rounded rectangle track and progress
            drawRoundedRectProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius,
                isWavy = true,
                waveOffset = phaseShift,
                waveAmplitude = waveAmplitudeAnim
            )
        } else {
            // Original circular implementation
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw wavy progress (wave flattens to circle when paused)
            if (progress > 0f) {
                val path = Path()
                val sweepAngle = 360f * progress
                val steps = 200
                
                var prevX = 0f
                var prevY = 0f
                
                for (i in 0..steps) {
                    val angle = (i.toFloat() / steps) * sweepAngle
                    if (angle > sweepAngle) break
                    
                    val angleRad = Math.toRadians((angle - 90).toDouble())
                    val wave = sin((angle / 360f * 12 * PI) + phaseShift).toFloat() * stroke * waveAmplitudeAnim
                    val currentRadius = radius + wave
                    
                    val x = center.x + (currentRadius * kotlin.math.cos(angleRad)).toFloat()
                    val y = center.y + (currentRadius * kotlin.math.sin(angleRad)).toFloat()
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                        prevX = x
                        prevY = y
                    } else {
                        // Use quadratic bezier for smoother curves
                        val midX = (prevX + x) * 0.5f
                        val midY = (prevY + y) * 0.5f
                        path.quadraticTo(prevX, prevY, midX, midY)
                        prevX = x
                        prevY = y
                    }
                }
                
                drawPath(
                    path = path,
                    color = progressColor,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        miter = 1f
                    )
                )
            }
        }
    }
}

@Composable
private fun SegmentedCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectSegmentedProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            val segments = 20
            val segmentAngle = 360f / segments
            val gapAngle = 4f
            
            for (i in 0 until segments) {
                val startAngle = i * segmentAngle - 90f
                val segmentProgress = ((progress * segments) - i).coerceIn(0f, 1f)
                
                drawArc(
                    color = if (segmentProgress > 0) progressColor else trackColor,
                    startAngle = startAngle + gapAngle / 2,
                    sweepAngle = (segmentAngle - gapAngle) * segmentProgress.coerceAtLeast(0.01f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

@Composable
private fun DottedCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectDottedProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            val dots = 24
            val dotRadius = stroke * 0.8f
            
            for (i in 0 until dots) {
                val angle = (i.toFloat() / dots) * 360f - 90f
                val angleRad = Math.toRadians(angle.toDouble())
                val dotProgress = ((progress * dots) - i).coerceIn(0f, 1f)
                
                val x = center.x + (radius * kotlin.math.cos(angleRad)).toFloat()
                val y = center.y + (radius * kotlin.math.sin(angleRad)).toFloat()
                
                drawCircle(
                    color = if (dotProgress > 0) progressColor else trackColor,
                    radius = dotRadius * (0.5f + dotProgress * 0.5f),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun GradientCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectGradientProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw gradient progress
            if (progress > 0f) {
                val sweepAngle = 360f * progress
                
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            progressColor,
                            progressColor.copy(alpha = 0.7f),
                            progressColor.copy(alpha = 0.9f),
                            progressColor
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

@Composable
private fun NormalCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius,
                isWavy = false,
                waveOffset = 0f
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw progress arc
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

@Composable
private fun ThinCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    NormalCircularProgress(
        progress = progress,
        progressColor = progressColor,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        cornerRadius = cornerRadius
    )
}

@Composable
private fun ThickCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    NormalCircularProgress(
        progress = progress,
        progressColor = progressColor,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        cornerRadius = cornerRadius
    )
}

@Composable
private fun RoundedCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius,
                isWavy = false,
                waveOffset = 0f,
                useRoundCap = true
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw progress arc with round cap
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

// Helper function to draw rounded rectangle progress
private fun DrawScope.drawRoundedRectProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float,
    isWavy: Boolean = false,
    waveOffset: Float = 0f,
    useRoundCap: Boolean = false,
    waveAmplitude: Float = 0.2f
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    // Draw track as rounded rect stroke
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cr),
        style = Stroke(width = strokeWidth)
    )
    
    // Draw progress - trace the rounded rect perimeter
    if (progress > 0f) {
        val path = createRoundedRectProgressPath(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            cornerRadius = cr,
            progress = progress,
            isWavy = isWavy,
            waveOffset = waveOffset,
            strokeWidth = strokeWidth,
            waveAmplitude = waveAmplitude
        )
        
        drawPath(
            path = path,
            color = progressColor,
            style = Stroke(
                width = strokeWidth,
                cap = if (useRoundCap) StrokeCap.Round else StrokeCap.Butt
            )
        )
    }
}

private fun DrawScope.drawRoundedRectSegmentedProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    // Draw track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cr),
        style = Stroke(width = strokeWidth)
    )
    
    // Calculate perimeter for segmentation
    val segments = 20
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cr)
    val segmentLength = perimeter / segments
    val gapLength = segmentLength * 0.15f
    
    for (i in 0 until segments) {
        val segmentProgress = ((progress * segments) - i).coerceIn(0f, 1f)
        if (segmentProgress > 0) {
            val startOffset = i * segmentLength + gapLength / 2
            val endOffset = startOffset + (segmentLength - gapLength) * segmentProgress
            
            val path = createRoundedRectSegmentPath(
                left = left, top = top, right = right, bottom = bottom,
                cornerRadius = cr, startOffset = startOffset, endOffset = endOffset
            )
            
            drawPath(
                path = path,
                color = progressColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    miter = 1f
                )
            )
        }
    }
}

private fun DrawScope.drawRoundedRectDottedProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    val dots = 24
    val dotRadius = strokeWidth * 0.8f
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cr)
    
    for (i in 0 until dots) {
        val offset = (i.toFloat() / dots) * perimeter
        val point = getPointOnRoundedRect(left, top, right, bottom, cr, offset)
        val dotProgress = ((progress * dots) - i).coerceIn(0f, 1f)
        
        drawCircle(
            color = if (dotProgress > 0) progressColor else trackColor,
            radius = dotRadius * (0.5f + dotProgress * 0.5f),
            center = point
        )
    }
}

private fun DrawScope.drawRoundedRectGradientProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    // Draw track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cr),
        style = Stroke(width = strokeWidth)
    )
    
    // Draw gradient progress
    if (progress > 0f) {
        val path = createRoundedRectProgressPath(
            left = left, top = top, right = right, bottom = bottom,
            cornerRadius = cr, progress = progress,
            isWavy = false, waveOffset = 0f, strokeWidth = strokeWidth
        )
        
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    progressColor,
                    progressColor.copy(alpha = 0.7f),
                    progressColor.copy(alpha = 0.9f),
                    progressColor
                )
            ),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                miter = 1f
            )
        )
    }
}

// Helper function to create path for rounded rect progress
private fun DrawScope.createRoundedRectProgressPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    progress: Float,
    isWavy: Boolean,
    waveOffset: Float,
    strokeWidth: Float,
    waveAmplitude: Float = 0.2f
): Path {
    val path = Path()
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cornerRadius)
    val targetLength = perimeter * progress
    
    var currentLength = 0f
    val steps = 200
    val stepLength = perimeter / steps
    
    for (i in 0..steps) {
        if (currentLength > targetLength) break
        
        val offset = i * stepLength
        var point = getPointOnRoundedRect(left, top, right, bottom, cornerRadius, offset)
        
        if (isWavy && waveAmplitude > 0f) {
            // Add wave effect - amplitude controls waviness (0 = flat circle, 0.2+ = wavy)
            val wave = sin((offset / perimeter * 12 * PI) + waveOffset).toFloat() * strokeWidth * waveAmplitude
            // Apply wave perpendicular to path
            val nextPoint = getPointOnRoundedRect(left, top, right, bottom, cornerRadius, (offset + 1).coerceAtMost(perimeter))
            val dx = nextPoint.x - point.x
            val dy = nextPoint.y - point.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            point = Offset(
                point.x + (-dy / len) * wave,
                point.y + (dx / len) * wave
            )
        }
        
        if (i == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
        
        currentLength += stepLength
    }
    
    return path
}

private fun DrawScope.createRoundedRectSegmentPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    startOffset: Float,
    endOffset: Float
): Path {
    val path = Path()
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cornerRadius)
    
    val steps = 20
    val length = endOffset - startOffset
    val stepLength = length / steps
    
    for (i in 0..steps) {
        val offset = (startOffset + i * stepLength).coerceAtMost(perimeter)
        val point = getPointOnRoundedRect(left, top, right, bottom, cornerRadius, offset)
        
        if (i == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    
    return path
}

private fun calculateRoundedRectPerimeter(width: Float, height: Float, cornerRadius: Float): Float {
    val cornerArc = 2 * PI.toFloat() * cornerRadius / 4 // Quarter circle
    val straightWidth = (width - 2 * cornerRadius).coerceAtLeast(0f)
    val straightHeight = (height - 2 * cornerRadius).coerceAtLeast(0f)
    return 4 * cornerArc + 2 * straightWidth + 2 * straightHeight
}

private fun getPointOnRoundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    offset: Float
): Offset {
    val width = right - left
    val height = bottom - top
    val cr = cornerRadius.coerceAtMost(kotlin.math.min(width, height) / 2)
    
    val cornerArc = PI.toFloat() * cr / 2
    val straightWidth = (width - 2 * cr).coerceAtLeast(0f)
    val straightHeight = (height - 2 * cr).coerceAtLeast(0f)
    val perimeter = 4 * cornerArc + 2 * straightWidth + 2 * straightHeight
    
    var pos = offset % perimeter
    if (pos < 0) pos += perimeter
    
    // Start from top center, go clockwise
    val topCenterX = left + width / 2
    
    // Top edge (right half)
    val topRightStraight = straightWidth / 2
    if (pos < topRightStraight) {
        return Offset(topCenterX + pos, top)
    }
    pos -= topRightStraight
    
    // Top-right corner
    if (pos < cornerArc) {
        val angle = -PI.toFloat() / 2 + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            right - cr + cr * kotlin.math.cos(angle),
            top + cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Right edge
    if (pos < straightHeight) {
        return Offset(right, top + cr + pos)
    }
    pos -= straightHeight
    
    // Bottom-right corner
    if (pos < cornerArc) {
        val angle = 0f + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            right - cr + cr * kotlin.math.cos(angle),
            bottom - cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Bottom edge
    if (pos < straightWidth) {
        return Offset(right - cr - pos, bottom)
    }
    pos -= straightWidth
    
    // Bottom-left corner
    if (pos < cornerArc) {
        val angle = PI.toFloat() / 2 + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            left + cr + cr * kotlin.math.cos(angle),
            bottom - cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Left edge
    if (pos < straightHeight) {
        return Offset(left, bottom - cr - pos)
    }
    pos -= straightHeight
    
    // Top-left corner
    if (pos < cornerArc) {
        val angle = PI.toFloat() + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            left + cr + cr * kotlin.math.cos(angle),
            top + cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Top edge (left half)
    return Offset(left + cr + pos, top)
}

