/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme.festive

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Snowflake display area options
 */
enum class SnowflakeArea {
    FULL_SCREEN,      // Entire screen
    LEFT_RIGHT_ONLY,  // Only left and right 25% of screen
    TOP_ONE_THIRD     // Top 1/3 of screen
}

/**
 * Data class representing a single snowflake
 */
data class Snowflake(
    val id: Int,
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val opacity: Float,
    val color: Color
) {
    var swayOffset: Float = 0f
    var currentRotation: Float = rotation
    
    /**
     * Update snowflake position based on time
     */
    fun update(deltaTime: Float, screenWidth: Float, screenHeight: Float) {
        // Update vertical position
        y += speed * deltaTime
        
        // Update horizontal sway
        swayOffset += swayFrequency * deltaTime
        x += swayAmplitude * cos(swayOffset) * deltaTime
        
        // Update rotation
        currentRotation += rotationSpeed * deltaTime
        
        // Reset if gone off screen
        if (y > screenHeight + size) {
            y = -size
            x = Random.nextFloat() * screenWidth
            swayOffset = 0f
        }
        
        // Wrap around horizontally
        if (x < -size) {
            x = screenWidth + size
        } else if (x > screenWidth + size) {
            x = -size
        }
    }
    
    /**
     * Get the current position as Offset
     */
    fun getPosition(): Offset = Offset(x, y)
}

/**
 * Generator for creating snowflakes with various properties
 */
object SnowflakeGenerator {
    
    /**
     * Generate a collection of snowflakes
     */
    fun generateSnowflakes(
        count: Int,
        screenWidth: Float,
        screenHeight: Float,
        intensity: Float = 0.5f,
        sizeMultiplier: Float = 1.0f,
        area: SnowflakeArea = SnowflakeArea.FULL_SCREEN
    ): List<Snowflake> {
        val snowflakes = mutableListOf<Snowflake>()
        
        for (i in 0 until count) {
            snowflakes.add(createSnowflake(i, screenWidth, screenHeight, intensity, sizeMultiplier, area))
        }
        
        return snowflakes
    }
    
    /**
     * Create a single snowflake with random properties
     */
    private fun createSnowflake(
        id: Int,
        screenWidth: Float,
        screenHeight: Float,
        intensity: Float,
        sizeMultiplier: Float = 1.0f,
        area: SnowflakeArea = SnowflakeArea.FULL_SCREEN
    ): Snowflake {
        val random = Random(System.currentTimeMillis() + id)
        
        // Size varies from small to medium-large based on intensity and multiplier
        val minSize = 4f * sizeMultiplier
        val maxSize = (12f + (intensity * 8f)) * sizeMultiplier
        val size = minSize + random.nextFloat() * (maxSize - minSize)
        
        // Speed varies with size (larger = slower fall)
        val baseSpeed = 20f + (intensity * 30f)
        val speed = baseSpeed * (1.0f - (size - minSize) / (maxSize - minSize) * 0.5f)
        
        // Horizontal sway
        val swayAmplitude = 10f + random.nextFloat() * 20f
        val swayFrequency = 0.5f + random.nextFloat() * 1.5f
        
        // Rotation
        val rotation = random.nextFloat() * 360f
        val rotationSpeed = (random.nextFloat() - 0.5f) * 100f // Can rotate both ways
        
        // Opacity varies
        val opacity = 0.5f + random.nextFloat() * 0.5f
        
        // Color variations (white to light blue)
        val colorVariant = random.nextInt(3)
        val color = when (colorVariant) {
            0 -> Color.White.copy(alpha = opacity)
            1 -> Color(0xFFE0F2FE).copy(alpha = opacity) // Light blue
            else -> Color(0xFFF0F9FF).copy(alpha = opacity) // Very light blue
        }
        
        // Initial position based on area
        val x = when (area) {
            SnowflakeArea.LEFT_RIGHT_ONLY -> {
                // Left 25% or right 25% of screen
                if (random.nextBoolean()) {
                    random.nextFloat() * (screenWidth * 0.25f) // Left quarter
                } else {
                    screenWidth * 0.75f + random.nextFloat() * (screenWidth * 0.25f) // Right quarter
                }
            }
            else -> random.nextFloat() * screenWidth // Full width or top two thirds
        }
        val y = -size - random.nextFloat() * screenHeight // Spread vertically at start
        
        return Snowflake(
            id = id,
            x = x,
            y = y,
            size = size,
            speed = speed,
            swayAmplitude = swayAmplitude,
            swayFrequency = swayFrequency,
            rotation = rotation,
            rotationSpeed = rotationSpeed,
            opacity = opacity,
            color = color
        )
    }
    
    /**
     * Calculate optimal snowflake count based on screen size and intensity
     */
    fun calculateSnowflakeCount(
        screenWidth: Float,
        screenHeight: Float,
        intensity: Float
    ): Int {
        val screenArea = screenWidth * screenHeight
        val baseCount = (screenArea / 15000f).toInt() // Base density
        val intensityMultiplier = 0.5f + intensity // 0.5x to 1.5x
        return (baseCount * intensityMultiplier).toInt().coerceIn(20, 150)
    }
}

/**
 * Snowflake shape variations for drawing
 */
object SnowflakeShapes {
    
    /**
     * Generate points for a snowflake shape
     * Returns list of offsets relative to center
     */
    fun getSnowflakePoints(size: Float, arms: Int = 6): List<Offset> {
        val points = mutableListOf<Offset>()
        val angleStep = (2 * PI / arms).toFloat()
        
        for (i in 0 until arms) {
            val angle = i * angleStep
            
            // Main arm
            val armLength = size / 2
            val endX = cos(angle) * armLength
            val endY = sin(angle) * armLength
            points.add(Offset(endX, endY))
            
            // Side branches
            val branchLength = armLength * 0.4f
            val branchAngle1 = angle + PI.toFloat() / 6
            val branchAngle2 = angle - PI.toFloat() / 6
            
            points.add(Offset(
                cos(branchAngle1) * branchLength,
                sin(branchAngle1) * branchLength
            ))
            points.add(Offset(
                cos(branchAngle2) * branchLength,
                sin(branchAngle2) * branchLength
            ))
        }
        
        return points
    }
    
    /**
     * Simple circle shape for performance
     */
    fun getCircleShape(size: Float): Float = size / 2
}
