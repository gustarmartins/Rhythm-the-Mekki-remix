/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme.festive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.shared.data.model.AppSettings

/**
 * Festive Overlay - Wraps content with festive decorations
 * Automatically applies appropriate festive effects based on configuration
 */
@Composable
fun FestiveOverlay(
    config: FestiveConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val activeFestiveTheme = FestiveThemeEngine.getActiveFestiveTheme(config)
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render main content
        content()
        
        // Render festive effect overlay based on active theme
        // Use key to force recomposition when intensity or theme changes
        androidx.compose.runtime.key(config.type, config.intensity) {
            when (activeFestiveTheme) {
                FestiveThemeType.CHRISTMAS -> {
                    // Snowfall effect in background
                    SnowfallWithSparkle(
                        intensity = config.intensity,
                        enabled = true,
                        detailedRendering = false
                    )
                    // Christmas decorations overlay
                    ChristmasDecorations(intensity = config.intensity)
                }
                FestiveThemeType.NEW_YEAR -> {
                    // New Year can also use snowfall or confetti
                    SnowfallWithSparkle(
                        intensity = config.intensity * 1.2f, // More intense
                        enabled = true,
                        detailedRendering = false
                    )
                    // Snow collection at bottom
                    Box(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                        SnowCollection(intensity = config.intensity)
                    }
                }
                FestiveThemeType.HALLOWEEN -> {
                    // Placeholder for future Halloween effects (falling leaves, bats, etc.)
                    // TODO: Implement Halloween effects
                }
                FestiveThemeType.VALENTINES -> {
                    // Placeholder for future Valentine's effects (hearts, rose petals, etc.)
                    // TODO: Implement Valentine's effects
                }
                FestiveThemeType.CUSTOM -> {
                    // Custom festive effects based on user preference
                    SnowfallWithSparkle(
                        intensity = config.intensity,
                        enabled = true,
                        detailedRendering = false
                    )
                }
                FestiveThemeType.NONE -> {
                    // No festive decorations
                }
            }
        }
    }
}

/**
 * Festive Overlay with AppSettings integration
 * Automatically reads configuration from app settings
 */
@Composable
fun FestiveOverlayFromSettings(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    
    val festiveEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveType by appSettings.festiveThemeType.collectAsState()
    val festiveIntensity by appSettings.festiveThemeIntensity.collectAsState()
    val festiveAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()
    val snowflakeSize by appSettings.festiveSnowflakeSize.collectAsState()
    val snowflakeArea by appSettings.festiveSnowflakeArea.collectAsState()
    
    // Decoration position settings
    val showTopLights by appSettings.festiveShowTopLights.collectAsState()
    val showSideGarland by appSettings.festiveShowSideGarland.collectAsState()
    val showBottomSnow by appSettings.festiveShowBottomSnow.collectAsState()
    val showSnowfall by appSettings.festiveShowSnowfall.collectAsState()
    
    // Safe parse of festive theme type with fallback to NONE if invalid
    val themType = try {
        FestiveThemeType.valueOf(festiveType)
    } catch (e: Exception) {
        // Fallback when stored preference is outdated or invalid (e.g., DIWALI removed)
        FestiveThemeType.NONE
    }
    
    val config = FestiveConfig(
        type = themType,
        intensity = festiveIntensity,
        enabled = festiveEnabled,
        autoDetect = festiveAutoDetect
    )
    
    val areaEnum = try {
        SnowflakeArea.valueOf(snowflakeArea)
    } catch (e: Exception) {
        SnowflakeArea.FULL_SCREEN
    }
    
    FestiveOverlayWithParams(
        config = config,
        snowflakeSize = snowflakeSize,
        snowflakeArea = areaEnum,
        showTopLights = showTopLights,
        showSideGarland = showSideGarland,
        showBottomSnow = showBottomSnow,
        showSnowfall = showSnowfall,
        modifier = modifier,
        content = content
    )
}

/**
 * Festive Overlay with additional parameters for snowflake customization
 */
@Composable
fun FestiveOverlayWithParams(
    config: FestiveConfig,
    modifier: Modifier = Modifier,
    snowflakeSize: Float = 1.0f,
    snowflakeArea: SnowflakeArea = SnowflakeArea.FULL_SCREEN,
    showTopLights: Boolean = true,
    showSideGarland: Boolean = true,
    showBottomSnow: Boolean = true,
    showSnowfall: Boolean = true,
    content: @Composable () -> Unit
) {
    val activeFestiveTheme = FestiveThemeEngine.getActiveFestiveTheme(config)
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render main content
        content()
        
        // Render festive effect overlay based on active theme
        // Use key to force recomposition when intensity or theme changes
        androidx.compose.runtime.key(config.type, config.intensity, snowflakeSize, snowflakeArea, showTopLights, showSideGarland, showBottomSnow, showSnowfall) {
            when (activeFestiveTheme) {
                FestiveThemeType.CHRISTMAS -> {
                    // Snowfall effect in background (only if enabled)
                    if (showSnowfall) {
                        SnowfallWithSparkle(
                            intensity = config.intensity,
                            enabled = true,
                            detailedRendering = false,
                            sizeMultiplier = snowflakeSize,
                            area = snowflakeArea
                        )
                    }
                    // Christmas decorations overlay with position settings
                    ChristmasDecorations(
                        intensity = config.intensity,
                        showTopLights = showTopLights,
                        showSideGarland = showSideGarland,
                        showBottomSnow = showBottomSnow
                    )
                }
                FestiveThemeType.NEW_YEAR -> {
                    // New Year can also use snowfall or confetti (only if enabled)
                    if (showSnowfall) {
                        SnowfallWithSparkle(
                            intensity = config.intensity * 1.2f, // More intense
                            enabled = true,
                            detailedRendering = false,
                            sizeMultiplier = snowflakeSize,
                            area = snowflakeArea
                        )
                    }
                    // Snow collection at bottom (only if enabled)
                    if (showBottomSnow) {
                        Box(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                            SnowCollection(intensity = config.intensity)
                        }
                    }
                }
                FestiveThemeType.HALLOWEEN -> {
                    // Placeholder for future Halloween effects (falling leaves, bats, etc.)
                    // TODO: Implement Halloween effects
                }
                FestiveThemeType.VALENTINES -> {
                    // Placeholder for future Valentine's effects (hearts, rose petals, etc.)
                    // TODO: Implement Valentine's effects
                }
                FestiveThemeType.CUSTOM -> {
                    // Custom festive effects based on user preference
                    if (showSnowfall) {
                        SnowfallWithSparkle(
                            intensity = config.intensity,
                            enabled = true,
                            detailedRendering = false,
                            sizeMultiplier = snowflakeSize,
                            area = snowflakeArea
                        )
                    }
                }
                FestiveThemeType.NONE -> {
                    // No festive decorations
                }
            }
        }
    }
}

/**
 * Simple Christmas Snowfall Overlay
 * Quick helper for applying Christmas snowfall decoration
 */
@Composable
fun ChristmasSnowfallOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        
        if (enabled) {
            SnowfallWithSparkle(
                intensity = intensity,
                enabled = true,
                detailedRendering = false
            )
        }
    }
}

/**
 * Performance-optimized festive overlay
 * Uses simple rendering for better performance
 */
@Composable
fun LightweightFestiveOverlay(
    config: FestiveConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val activeFestiveTheme = FestiveThemeEngine.getActiveFestiveTheme(config)
    
    Box(modifier = modifier.fillMaxSize()) {
        content()
        
        // Only use lightweight effects
        when (activeFestiveTheme) {
            FestiveThemeType.CHRISTMAS,
            FestiveThemeType.NEW_YEAR,
            FestiveThemeType.CUSTOM -> {
                SnowfallEffect(
                    intensity = config.intensity * 0.8f, // Reduce intensity for performance
                    enabled = true
                )
            }
            else -> {
                // No effects
            }
        }
    }
}

/**
 * Preview helper for testing festive effects
 */
@Composable
fun FestiveOverlayPreview(
    festiveType: FestiveThemeType,
    intensity: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val config = FestiveConfig(
        type = festiveType,
        intensity = intensity,
        enabled = true,
        autoDetect = false
    )
    
    FestiveOverlay(
        config = config,
        content = content
    )
}
