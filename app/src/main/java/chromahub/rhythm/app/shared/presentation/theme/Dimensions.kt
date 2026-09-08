/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Spacing System
 * Consistent spacing values following the 4dp grid system
 */
object Spacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
    val giant = 64.dp
}

/**
 * Music app specific dimensions
 */
object MusicDimensions {
    // Player components
    val miniPlayerHeight = 56.dp
    val fullPlayerHeight = 480.dp
    val albumCoverSize = 280.dp
    val albumCoverSizeMini = 48.dp
    val playerControlSize = 56.dp
    val playerControlSizeSmall = 40.dp
    
    // Navigation and layout - responsive to DPI
    val bottomNavigationHeight: Dp
        @Composable
        get() = with(LocalDensity.current) {
            // Dynamic sizing based on screen density for optimal touch targets and screen usage
            val scaleFactor = when {
                density < 1.5f -> 1.3f  // Low density devices: larger for better touch targets
                density < 2.5f -> 1.1f  // Medium density devices: moderately larger
                else -> 0.9f            // High density devices: slightly smaller to save screen space
            }
            (66f * scaleFactor).dp
        }
    
    val topAppBarHeight = 64.dp
    val listItemHeight = 72.dp
    val listItemHeightCompact = 56.dp
    
    // Cards and containers
    val cardElevation = 2.dp
    val cardElevationPressed = 8.dp

    // Song info sheet metadata tiles
    val infoTileHeight = 80.dp
    val infoTileIconSize = 60.dp
    val infoTileInset = 12.dp
    val infoTileWatermarkAlpha = 0.14f
    val infoTileBackdropCut = 12.dp

    // Buttons and interactive elements
    val buttonHeight = 40.dp
    val buttonHeightLarge = 48.dp
    val fabSize = 56.dp
    val fabSizeSmall = 40.dp
}
