/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme.festive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random
import chromahub.rhythm.app.util.windowScreenWidthDp
import chromahub.rhythm.app.util.windowScreenHeightDp

/**
 * Complete Christmas decorations including lights, garland, and snow collection
 * Supports enabling/disabling individual decoration elements by position
 */
@Composable
fun ChristmasDecorations(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f,
    showTopLights: Boolean = true,
    showSideGarland: Boolean = true,
    showBottomSnow: Boolean = true
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top Christmas lights - below status bar
        if (showTopLights) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
            ) {
                ChristmasLights(intensity = intensity)
            }
        }
        
        // Side decorations (garland with ornaments)
        if (showSideGarland) {
            SideDecorations(intensity = intensity)
        }
        
        // Bottom snow collection - anchored to bottom
        if (showBottomSnow) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SnowCollection(intensity = intensity)
            }
        }
    }
}

/**
 * Christmas lights string at the top
 */
@Composable
fun ChristmasLights(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { windowScreenWidthDp().dp.toPx() }
    
    // Blinking animation for lights
    val infiniteTransition = rememberInfiniteTransition(label = "lights")
    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blinkPhase"
    )
    
    // Generate light positions
    val lights = remember(screenWidth, intensity) {
        val count = (screenWidth / 80f * (0.5f + intensity * 0.5f)).toInt().coerceIn(8, 20)
        List(count) { index ->
            val x = (screenWidth / count) * index + (screenWidth / count / 2)
            val colorIndex = index % 4
            val color = when (colorIndex) {
                0 -> Color(0xFFFF4444) // Red
                1 -> Color(0xFF44FF44) // Green
                2 -> Color(0xFFFFFF44) // Yellow
                3 -> Color(0xFF4444FF) // Blue
                else -> Color.White
            }
            Triple(x, color, index * 90f) // x, color, phase offset
        }
    }
    
    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        // Draw wavy wire/string
        val wireY = 40f
        val waveAmplitude = 8f
        val waveFrequency = 3f
        val path = Path().apply {
            moveTo(0f, wireY)
            var x = 0f
            while (x <= size.width) {
                val y = wireY + sin((x / size.width) * waveFrequency * 2 * PI).toFloat() * waveAmplitude
                lineTo(x, y)
                x += 10f
            }
        }
        
        drawPath(
            path = path,
            color = Color(0xFF2C5530).copy(alpha = 0.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
        
        // Draw lights hanging from wavy wire
        lights.forEach { (x, color, phaseOffset) ->
            val brightness = (sin((blinkPhase + phaseOffset) * PI / 180) * 0.3f + 0.7f).toFloat()
            val wireYAtX = wireY + sin((x / size.width) * waveFrequency * 2 * PI).toFloat() * waveAmplitude
            
            // Wire connection
            drawLine(
                color = Color(0xFF2C5530).copy(alpha = 0.6f),
                start = Offset(x, wireYAtX),
                end = Offset(x, wireYAtX + 15f),
                strokeWidth = 2f
            )
            
            // Light bulb
            drawCircle(
                color = color.copy(alpha = brightness),
                radius = 12f,
                center = Offset(x, wireYAtX + 25f)
            )
            
            // Glow effect
            drawCircle(
                color = color.copy(alpha = brightness * 0.3f),
                radius = 18f,
                center = Offset(x, wireYAtX + 25f)
            )
        }
    }
}

/**
 * Side decorations - Simple ornaments and garland style
 */
@Composable
fun SideDecorations(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f
) {
    val density = LocalDensity.current
    val screenHeight = with(density) { windowScreenHeightDp().dp.toPx() }
    
    // Gentle sway animation
    val infiniteTransition = rememberInfiniteTransition(label = "sideDecorations")
    val sway by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    
    // Twinkle animation for ornaments
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    
    // Generate more decorations with better variety
    val decorations = remember(screenHeight, intensity) {
        val count = (12 + (intensity * 8).toInt()).coerceIn(12, 20)
        val startY = 160f
        val endY = screenHeight - 250f
        
        List(count) { index ->
            // More even spacing with slight randomness
            val baseY = startY + ((endY - startY) / count) * index
            val y = baseY + (Random.nextFloat() * 40f - 20f)
            // Size variation 1.2-2.2x
            val size = 1.2f + Random.nextFloat() * 1.0f
            val colorIndex = index % 6
            val color = when (colorIndex) {
                0 -> Color(0xFFE63946) // Red
                1 -> Color(0xFFFFD700) // Gold
                2 -> Color(0xFF2A9D8F) // Teal/Green
                3 -> Color(0xFF4169E1) // Royal Blue
                4 -> Color(0xFFDC143C) // Crimson
                5 -> Color(0xFFFF1493) // Deep Pink
                else -> Color(0xFFFF6B6B)
            }
            // Add decoration type
            val decorationType = index % 6
            DecorationData(y, size, color, decorationType)
        }.sortedBy { it.y }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val leftX = 35f  // Slightly more inward
        val rightX = size.width - 35f
        
        // Left side - richer garland strand
        drawRichGarland(leftX, 140f, screenHeight - 250f, sway)
        
        // Large festive bow at top left
        drawFestiveBow(Offset(leftX + sway * 0.4f, 120f), Color(0xFFE63946), Color(0xFFFFD700), 2.5f)
        
        // Add gold star above bow
        drawStar(Offset(leftX + sway * 0.2f, 80f), Color(0xFFFFD700), 1.8f, twinkle)
        
        decorations.forEachIndexed { index, decoration ->
            val swayOffset = sway * (0.6f + decoration.size * 0.4f)
            
            // More variety in decorations
            when (decoration.type) {
                0 -> drawGlossyOrnament(
                    Offset(leftX + swayOffset, decoration.y),
                    decoration.color,
                    decoration.size,
                    twinkle
                )
                1 -> drawGlossyOrnament(
                    Offset(leftX + swayOffset, decoration.y),
                    decoration.color.copy(alpha = 0.9f),
                    decoration.size * 0.9f,
                    twinkle
                )
                2 -> drawCandyCane(
                    Offset(leftX + swayOffset, decoration.y),
                    decoration.size * 0.85f
                )
                3 -> drawHolly(
                    Offset(leftX + swayOffset, decoration.y),
                    decoration.size * 0.95f
                )
                4 -> drawStar(
                    Offset(leftX + swayOffset, decoration.y),
                    Color(0xFFFFD700),
                    decoration.size * 0.8f,
                    twinkle
                )
                5 -> drawGingerbread(
                    Offset(leftX + swayOffset, decoration.y),
                    decoration.size * 0.85f
                )
            }
        }
        
        // Right side - matching rich garland
        drawRichGarland(rightX, 140f, screenHeight - 250f, -sway)
        
        // Large festive bow at top right
        drawFestiveBow(Offset(rightX - sway * 0.4f, 120f), Color(0xFF2A9D8F), Color(0xFFFFD700), 2.5f)
        
        // Add gold star above bow
        drawStar(Offset(rightX - sway * 0.2f, 80f), Color(0xFFFFD700), 1.8f, twinkle)
        
        decorations.forEachIndexed { index, decoration ->
            val colorIndex = (index + 3) % 6
            val color = when (colorIndex) {
                0 -> Color(0xFFE63946)
                1 -> Color(0xFFFFD700)
                2 -> Color(0xFF2A9D8F)
                3 -> Color(0xFF4169E1)
                4 -> Color(0xFFDC143C)
                5 -> Color(0xFFFF1493)
                else -> Color(0xFFFF6B6B)
            }
            val swayOffset = -sway * (0.6f + decoration.size * 0.4f)
            
            // Mirror the decoration types
            when (decoration.type) {
                0 -> drawGlossyOrnament(
                    Offset(rightX + swayOffset, decoration.y),
                    color,
                    decoration.size,
                    twinkle
                )
                1 -> drawGlossyOrnament(
                    Offset(rightX + swayOffset, decoration.y),
                    color.copy(alpha = 0.9f),
                    decoration.size * 0.9f,
                    twinkle
                )
                2 -> drawCandyCane(
                    Offset(rightX + swayOffset, decoration.y),
                    decoration.size * 0.85f
                )
                3 -> drawHolly(
                    Offset(rightX + swayOffset, decoration.y),
                    decoration.size * 0.95f
                )
                4 -> drawStar(
                    Offset(rightX + swayOffset, decoration.y),
                    Color(0xFFFFD700),
                    decoration.size * 0.8f,
                    twinkle
                )
                5 -> drawGingerbread(
                    Offset(rightX + swayOffset, decoration.y),
                    decoration.size * 0.85f
                )
            }
        }
    }
}

/**
 * Helper data class for decorations
 */
private data class DecorationData(val y: Float, val size: Float, val color: Color, val type: Int)

/**
 * Draw a vertical garland strand
 */
private fun DrawScope.drawVerticalGarland(x: Float, startY: Float, endY: Float, sway: Float) {
    val segments = 30
    val path = Path()
    path.moveTo(x, startY)
    
    for (i in 0..segments) {
        val progress = i.toFloat() / segments
        val y = startY + (endY - startY) * progress
        val swayAmount = sin(progress * PI * 4).toFloat() * 8f + sway * 0.3f
        path.lineTo(x + swayAmount, y)
    }
    
    // Dark green garland strand
    drawPath(
        path = path,
        color = Color(0xFF1B4332),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
}

/**
 * Draw a decorative bow
 */
private fun DrawScope.drawBow(center: Offset, color: Color, scale: Float = 1f) {
    val size = 18f * scale
    
    // Left loop
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0.7f)),
            center = Offset(center.x - size * 0.3f, center.y),
            radius = size * 0.4f
        ),
        radius = size * 0.4f,
        center = Offset(center.x - size * 0.3f, center.y)
    )
    
    // Right loop
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0.7f)),
            center = Offset(center.x + size * 0.3f, center.y),
            radius = size * 0.4f
        ),
        radius = size * 0.4f,
        center = Offset(center.x + size * 0.3f, center.y)
    )
    
    // Center knot
    drawCircle(
        color = color,
        radius = size * 0.25f,
        center = center
    )
    
    // Ribbons hanging down
    val ribbon1 = Path().apply {
        moveTo(center.x - size * 0.15f, center.y + size * 0.2f)
        lineTo(center.x - size * 0.1f, center.y + size * 0.8f)
        lineTo(center.x, center.y + size * 0.7f)
        close()
    }
    drawPath(ribbon1, color = color.copy(alpha = 0.8f))
    
    val ribbon2 = Path().apply {
        moveTo(center.x + size * 0.15f, center.y + size * 0.2f)
        lineTo(center.x + size * 0.1f, center.y + size * 0.8f)
        lineTo(center.x, center.y + size * 0.7f)
        close()
    }
    drawPath(ribbon2, color = color.copy(alpha = 0.8f))
}

/**
 * Draw a candy cane
 */
private fun DrawScope.drawCandyCane(center: Offset, scale: Float = 1f) {
    val height = 20f * scale
    val width = 6f * scale
    
    // Draw candy cane shape
    val path = Path().apply {
        moveTo(center.x - width / 2, center.y + height * 0.5f)
        lineTo(center.x - width / 2, center.y - height * 0.2f)
        // Hook at top
        cubicTo(
            center.x - width / 2, center.y - height * 0.5f,
            center.x + width * 1.5f, center.y - height * 0.5f,
            center.x + width * 1.5f, center.y - height * 0.2f
        )
        lineTo(center.x + width / 2, center.y - height * 0.2f)
        // Inner curve
        cubicTo(
            center.x + width / 2, center.y - height * 0.35f,
            center.x + width / 2, center.y - height * 0.35f,
            center.x + width / 2, center.y - height * 0.2f
        )
        lineTo(center.x + width / 2, center.y + height * 0.5f)
        close()
    }
    
    // White background
    drawPath(path, color = Color.White)
    
    // Red stripes
    for (i in 0..4) {
        val y = center.y + height * 0.5f - (i * height * 0.2f)
        if (y > center.y - height * 0.2f) {
            drawRect(
                color = Color(0xFFE63946),
                topLeft = Offset(center.x - width / 2, y - height * 0.08f),
                size = androidx.compose.ui.geometry.Size(width, height * 0.08f)
            )
        }
    }
}

/**
 * Draw holly leaves and berries
 */
private fun DrawScope.drawHolly(center: Offset, scale: Float = 1f) {
    val leafSize = 12f * scale
    
    // Left leaf
    val leftLeaf = Path().apply {
        moveTo(center.x - leafSize * 0.3f, center.y)
        cubicTo(
            center.x - leafSize * 0.8f, center.y - leafSize * 0.3f,
            center.x - leafSize * 0.9f, center.y + leafSize * 0.3f,
            center.x - leafSize * 0.3f, center.y
        )
    }
    drawPath(leftLeaf, color = Color(0xFF2D5016))
    
    // Right leaf
    val rightLeaf = Path().apply {
        moveTo(center.x + leafSize * 0.3f, center.y)
        cubicTo(
            center.x + leafSize * 0.8f, center.y - leafSize * 0.3f,
            center.x + leafSize * 0.9f, center.y + leafSize * 0.3f,
            center.x + leafSize * 0.3f, center.y
        )
    }
    drawPath(rightLeaf, color = Color(0xFF2D5016))
    
    // Red berries
    drawCircle(
        color = Color(0xFFE63946),
        radius = 3f * scale,
        center = Offset(center.x - leafSize * 0.15f, center.y - leafSize * 0.3f)
    )
    drawCircle(
        color = Color(0xFFE63946),
        radius = 3f * scale,
        center = Offset(center.x + leafSize * 0.15f, center.y - leafSize * 0.3f)
    )
    drawCircle(
        color = Color(0xFFDC143C),
        radius = 3f * scale,
        center = Offset(center.x, center.y - leafSize * 0.4f)
    )
}

/**
 * Draw a simple ornament ball
 */
private fun DrawScope.drawSimpleOrnament(center: Offset, color: Color, scale: Float = 1f) {
    val radius = 12f * scale
    
    // Main ornament ball with gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 1f),
                color.copy(alpha = 0.8f)
            ),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius
        ),
        radius = radius,
        center = center
    )
    
    // Shine effect
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = 4f * scale,
        center = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f)
    )
    
    // Subtle outline
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
    )
}

/**
 * Snow collection at the bottom
 */
@Composable
fun SnowCollection(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { windowScreenWidthDp().dp.toPx() }
    
    // Generate snow pile pattern
    val snowPile = remember(screenWidth, intensity) {
        val points = 50
        List(points) { index ->
            val x = (screenWidth / points) * index
            val baseHeight = 30f + (intensity * 40f)
            val variation = (sin((index.toFloat() / points) * PI * 4) * 15f).toFloat()
            Offset(x, baseHeight + variation)
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((80f + intensity * 40f).dp)
    ) {
        // Draw snow pile
        val path = Path().apply {
            moveTo(0f, size.height)
            snowPile.forEach { point ->
                lineTo(point.x, size.height - point.y)
            }
            lineTo(size.width, size.height)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFAFA),
                    Color(0xFFF0F8FF)
                )
            )
        )
        
        // Add sparkles on snow
        val sparkleCount = (screenWidth / 100f * intensity).toInt()
        repeat(sparkleCount) { index ->
            val x = Random.nextFloat() * size.width
            val y = size.height - (30f + Random.nextFloat() * 40f * intensity)
            
            // Small sparkle
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2f,
                center = Offset(x, y)
            )
            
            // Glow
            drawCircle(
                color = Color(0xFFE0F2FF).copy(alpha = 0.4f),
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Draw a richer garland with pine needles effect
 */
private fun DrawScope.drawRichGarland(x: Float, startY: Float, endY: Float, sway: Float) {
    val segments = 40
    val segmentHeight = (endY - startY) / segments
    
    for (i in 0..segments) {
        val y = startY + (i * segmentHeight)
        val swayOffset = sin((i.toFloat() / segments) * PI * 4) * sway * 0.8f
        val currentX = x + swayOffset.toFloat()
        
        // Thicker garland strand
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2D5016),
                    Color(0xFF1B3D0C)
                ),
                radius = 6f
            ),
            radius = 6f,
            center = Offset(currentX, y)
        )
        
        // Add pine needle texture
        if (i % 3 == 0) {
            repeat(6) { needleIndex ->
                val angle = (needleIndex * 60f + i * 15f) * PI / 180f
                val needleLength = 8f
                drawLine(
                    color = Color(0xFF2D5016).copy(alpha = 0.7f),
                    start = Offset(currentX, y),
                    end = Offset(
                        currentX + (cos(angle) * needleLength).toFloat(),
                        y + (sin(angle) * needleLength).toFloat()
                    ),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

/**
 * Draw a glossy ornament with shine effect and twinkle
 */
private fun DrawScope.drawGlossyOrnament(center: Offset, color: Color, scale: Float = 1f, twinkle: Float = 1f) {
    val radius = 14f * scale
    
    // Main ornament body with gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = twinkle),
                color.copy(alpha = twinkle * 0.8f)
            ),
            center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
            radius = radius
        ),
        radius = radius,
        center = center
    )
    
    // Highlight shine
    drawCircle(
        color = Color.White.copy(alpha = 0.6f * twinkle),
        radius = radius * 0.35f,
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
    )
    
    // Smaller shine
    drawCircle(
        color = Color.White.copy(alpha = 0.4f * twinkle),
        radius = radius * 0.15f,
        center = Offset(center.x + radius * 0.2f, center.y + radius * 0.2f)
    )
    
    // Ornament cap
    val capWidth = radius * 0.5f
    val capHeight = radius * 0.25f
    drawRect(
        color = Color(0xFFB8860B),
        topLeft = Offset(center.x - capWidth / 2, center.y - radius - capHeight),
        size = androidx.compose.ui.geometry.Size(capWidth, capHeight)
    )
    
    // Ornament hook
    drawLine(
        color = Color(0xFFB8860B),
        start = Offset(center.x, center.y - radius - capHeight),
        end = Offset(center.x, center.y - radius - capHeight - 8f * scale),
        strokeWidth = 2f
    )
}

/**
 * Draw a festive two-color bow
 */
private fun DrawScope.drawFestiveBow(center: Offset, color1: Color, color2: Color, scale: Float = 1f) {
    val size = 20f * scale
    
    // Left loop
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color1, color1.copy(alpha = 0.7f)),
            center = Offset(center.x - size * 0.35f, center.y),
            radius = size * 0.45f
        ),
        radius = size * 0.45f,
        center = Offset(center.x - size * 0.35f, center.y)
    )
    
    // Right loop
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color2, color2.copy(alpha = 0.7f)),
            center = Offset(center.x + size * 0.35f, center.y),
            radius = size * 0.45f
        ),
        radius = size * 0.45f,
        center = Offset(center.x + size * 0.35f, center.y)
    )
    
    // Center knot
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD700),
                Color(0xFFB8860B)
            ),
            radius = size * 0.3f
        ),
        radius = size * 0.3f,
        center = center
    )
    
    // Ribbons hanging down
    val ribbon1 = Path().apply {
        moveTo(center.x - size * 0.2f, center.y + size * 0.25f)
        lineTo(center.x - size * 0.15f, center.y + size * 0.9f)
        lineTo(center.x, center.y + size * 0.8f)
        close()
    }
    drawPath(ribbon1, color = color1.copy(alpha = 0.85f))
    
    val ribbon2 = Path().apply {
        moveTo(center.x + size * 0.2f, center.y + size * 0.25f)
        lineTo(center.x + size * 0.15f, center.y + size * 0.9f)
        lineTo(center.x, center.y + size * 0.8f)
        close()
    }
    drawPath(ribbon2, color = color2.copy(alpha = 0.85f))
}

/**
 * Draw a star with twinkle effect
 */
private fun DrawScope.drawStar(center: Offset, color: Color, scale: Float = 1f, twinkle: Float = 1f) {
    val outerRadius = 12f * scale
    val innerRadius = 5f * scale
    val points = 5
    
    val path = Path().apply {
        for (i in 0 until points * 2) {
            val angle = (i * PI / points - PI / 2).toFloat()
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = center.x + cos(angle) * radius
            val y = center.y + sin(angle) * radius
            
            if (i == 0) moveTo(x, y)
            else lineTo(x, y)
        }
        close()
    }
    
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = twinkle),
                color.copy(alpha = twinkle * 0.7f)
            ),
            radius = outerRadius
        )
    )
    
    // Center shine
    drawCircle(
        color = Color.White.copy(alpha = 0.8f * twinkle),
        radius = 3f * scale,
        center = center
    )
}

/**
 * Draw a gingerbread man
 */
private fun DrawScope.drawGingerbread(center: Offset, scale: Float = 1f) {
    val size = 16f * scale
    val gingerbreadColor = Color(0xFFCD853F)
    
    // Body
    drawCircle(
        color = gingerbreadColor,
        radius = size * 0.4f,
        center = Offset(center.x, center.y + size * 0.1f)
    )
    
    // Head
    drawCircle(
        color = gingerbreadColor,
        radius = size * 0.28f,
        center = Offset(center.x, center.y - size * 0.35f)
    )
    
    // Arms
    drawCircle(
        color = gingerbreadColor,
        radius = size * 0.15f,
        center = Offset(center.x - size * 0.45f, center.y)
    )
    drawCircle(
        color = gingerbreadColor,
        radius = size * 0.15f,
        center = Offset(center.x + size * 0.45f, center.y)
    )
    
    // Legs
    drawCircle(
        color = gingerbreadColor,
        radius = size * 0.15f,
        center = Offset(center.x - size * 0.2f, center.y + size * 0.55f)
    )
    drawCircle(
        color = gingerbreadColor,
        radius = size * 0.15f,
        center = Offset(center.x + size * 0.2f, center.y + size * 0.55f)
    )
    
    // Icing decoration
    val icingColor = Color.White
    // Eyes
    drawCircle(
        color = icingColor,
        radius = 2f * scale,
        center = Offset(center.x - size * 0.12f, center.y - size * 0.38f)
    )
    drawCircle(
        color = icingColor,
        radius = 2f * scale,
        center = Offset(center.x + size * 0.12f, center.y - size * 0.38f)
    )
    // Smile
    val smilePath = Path().apply {
        moveTo(center.x - size * 0.12f, center.y - size * 0.25f)
        quadraticTo(
            center.x, center.y - size * 0.2f,
            center.x + size * 0.12f, center.y - size * 0.25f
        )
    }
    drawPath(
        path = smilePath,
        color = icingColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale)
    )
    // Buttons
    drawCircle(
        color = icingColor,
        radius = 2f * scale,
        center = Offset(center.x, center.y + size * 0.05f)
    )
    drawCircle(
        color = icingColor,
        radius = 2f * scale,
        center = Offset(center.x, center.y + size * 0.25f)
    )
}

