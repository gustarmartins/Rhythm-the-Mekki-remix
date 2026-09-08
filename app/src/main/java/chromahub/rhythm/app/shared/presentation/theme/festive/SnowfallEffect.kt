/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme.festive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import chromahub.rhythm.app.util.windowScreenWidthDp
import chromahub.rhythm.app.util.windowScreenHeightDp

/**
 * Snowfall Effect Composable
 * Renders animated snowflakes across the screen
 */
@Composable
fun SnowfallEffect(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f,
    enabled: Boolean = true,
    sizeMultiplier: Float = 1.0f,
    area: SnowflakeArea = SnowflakeArea.FULL_SCREEN
) {
    if (!enabled) return
    
    val density = LocalDensity.current
    
    val screenWidth = with(density) { windowScreenWidthDp().dp.toPx() }
    val screenHeight = with(density) { windowScreenHeightDp().dp.toPx() }
    
    // Generate snowflakes
    val snowflakes = remember(intensity, sizeMultiplier, area) {
        val count = SnowflakeGenerator.calculateSnowflakeCount(
            screenWidth, 
            screenHeight, 
            intensity
        )
        SnowflakeGenerator.generateSnowflakes(
            count, 
            screenWidth, 
            screenHeight, 
            intensity,
            sizeMultiplier,
            area
        )
    }
    
    // Animation state
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var animationFrame by remember { mutableIntStateOf(0) }
    
    // Continuous animation loop
    LaunchedEffect(Unit) {
        // Reset lastFrameTime when effect starts to prevent large deltaTime on resume
        lastFrameTime = 0L
        
        while (isActive) {
            withFrameNanos { frameTime ->
                if (lastFrameTime != 0L) {
                    val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000f // Convert to seconds
                    
                    // Clamp deltaTime to prevent weird behavior when app is resumed after pause
                    val clampedDeltaTime = deltaTime.coerceIn(0f, 0.1f) // Max 100ms per frame
                    
                    // Update all snowflakes
                    snowflakes.forEach { snowflake ->
                        snowflake.update(clampedDeltaTime, screenWidth, screenHeight)
                    }
                    
                    animationFrame++
                }
                lastFrameTime = frameTime
            }
        }
    }
    
    // Draw snowflakes
    Canvas(modifier = modifier.fillMaxSize()) {
        snowflakes.forEach { snowflake ->
            // Only draw snowflakes in the specified area
            val shouldDraw = when (area) {
                SnowflakeArea.TOP_ONE_THIRD -> {
                    snowflake.y <= size.height * 0.333f
                }
                SnowflakeArea.LEFT_RIGHT_ONLY -> {
                    snowflake.x <= size.width * 0.25f || snowflake.x >= size.width * 0.75f
                }
                SnowflakeArea.FULL_SCREEN -> true
            }
            
            if (shouldDraw) {
                drawSnowflake(snowflake)
            }
        }
    }
}

/**
 * Draw a single snowflake
 */
private fun DrawScope.drawSnowflake(snowflake: Snowflake) {
    val position = snowflake.getPosition()
    
    // Simple circle rendering for performance
    // Can be enhanced with actual snowflake shapes
    drawCircle(
        color = snowflake.color,
        radius = snowflake.size / 2,
        center = position,
        alpha = snowflake.opacity
    )
    
    // Optional: Add a subtle glow effect for larger snowflakes
    if (snowflake.size > 8f) {
        drawCircle(
            color = Color.White,
            radius = snowflake.size / 3,
            center = position,
            alpha = snowflake.opacity * 0.3f
        )
    }
}

/**
 * Advanced snowflake drawing with rotation and detailed shape
 */
private fun DrawScope.drawDetailedSnowflake(snowflake: Snowflake) {
    val position = snowflake.getPosition()
    
    rotate(
        degrees = snowflake.currentRotation,
        pivot = position
    ) {
        // Draw main circle
        drawCircle(
            color = snowflake.color,
            radius = snowflake.size / 2,
            center = position,
            alpha = snowflake.opacity
        )
        
        // Draw snowflake arms
        val arms = 6
        val armLength = snowflake.size / 2
        val angleStep = 360f / arms
        
        for (i in 0 until arms) {
            val angle = Math.toRadians((i * angleStep).toDouble())
            val endX = position.x + (armLength * kotlin.math.cos(angle)).toFloat()
            val endY = position.y + (armLength * kotlin.math.sin(angle)).toFloat()
            
            drawLine(
                color = snowflake.color,
                start = position,
                end = Offset(endX, endY),
                strokeWidth = snowflake.size / 8,
                alpha = snowflake.opacity
            )
            
            // Draw side branches
            val branchLength = armLength * 0.4f
            val midX = position.x + (armLength * 0.6f * kotlin.math.cos(angle)).toFloat()
            val midY = position.y + (armLength * 0.6f * kotlin.math.sin(angle)).toFloat()
            
            val branchAngle1 = angle + Math.toRadians(30.0)
            val branchAngle2 = angle - Math.toRadians(30.0)
            
            drawLine(
                color = snowflake.color,
                start = Offset(midX, midY),
                end = Offset(
                    midX + (branchLength * kotlin.math.cos(branchAngle1)).toFloat(),
                    midY + (branchLength * kotlin.math.sin(branchAngle1)).toFloat()
                ),
                strokeWidth = snowflake.size / 12,
                alpha = snowflake.opacity * 0.8f
            )
            
            drawLine(
                color = snowflake.color,
                start = Offset(midX, midY),
                end = Offset(
                    midX + (branchLength * kotlin.math.cos(branchAngle2)).toFloat(),
                    midY + (branchLength * kotlin.math.sin(branchAngle2)).toFloat()
                ),
                strokeWidth = snowflake.size / 12,
                alpha = snowflake.opacity * 0.8f
            )
        }
    }
}

/**
 * Sparkle effect for snowflakes
 */
@Composable
fun SnowfallWithSparkle(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f,
    enabled: Boolean = true,
    detailedRendering: Boolean = false,
    sizeMultiplier: Float = 1.0f,
    area: SnowflakeArea = SnowflakeArea.FULL_SCREEN
) {
    if (!enabled) return
    
    val density = LocalDensity.current
    
    val screenWidth = with(density) { windowScreenWidthDp().dp.toPx() }
    val screenHeight = with(density) { windowScreenHeightDp().dp.toPx() }
    
    // Generate snowflakes
    val snowflakes = remember(intensity, sizeMultiplier, area) {
        val count = SnowflakeGenerator.calculateSnowflakeCount(
            screenWidth, 
            screenHeight, 
            intensity
        )
        SnowflakeGenerator.generateSnowflakes(
            count, 
            screenWidth, 
            screenHeight,
            intensity,
            sizeMultiplier,
            area
        )
    }
    
    // Sparkle animation
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )
    
    // Animation state
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var animationFrame by remember { mutableIntStateOf(0) }
    
    // Continuous animation loop
    LaunchedEffect(Unit) {
        // Reset lastFrameTime when effect starts to prevent large deltaTime on resume
        lastFrameTime = 0L
        
        while (isActive) {
            withFrameNanos { frameTime ->
                if (lastFrameTime != 0L) {
                    val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000f
                    
                    // Clamp deltaTime to prevent weird behavior when app is resumed after pause
                    val clampedDeltaTime = deltaTime.coerceIn(0f, 0.1f) // Max 100ms per frame
                    
                    snowflakes.forEach { snowflake ->
                        snowflake.update(clampedDeltaTime, screenWidth, screenHeight)
                    }
                    
                    animationFrame++
                }
                lastFrameTime = frameTime
            }
        }
    }
    
    // Draw snowflakes
    Canvas(modifier = modifier.fillMaxSize()) {
        snowflakes.forEachIndexed { index, snowflake ->
            // Only draw snowflakes in the specified area
            val shouldDraw = when (area) {
                SnowflakeArea.TOP_ONE_THIRD -> {
                    snowflake.y <= size.height * 0.333f
                }
                SnowflakeArea.LEFT_RIGHT_ONLY -> {
                    snowflake.x <= size.width * 0.25f || snowflake.x >= size.width * 0.75f
                }
                SnowflakeArea.FULL_SCREEN -> true
            }
            
            if (!shouldDraw) return@forEachIndexed
            
            // Add sparkle to some snowflakes
            val shouldSparkle = index % 5 == 0
            val adjustedSnowflake = if (shouldSparkle) {
                snowflake.copy(opacity = snowflake.opacity * sparkleAlpha)
            } else {
                snowflake
            }
            
            if (detailedRendering) {
                drawDetailedSnowflake(adjustedSnowflake)
            } else {
                drawSnowflake(adjustedSnowflake)
            }
        }
    }
}
