/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Extended Material Theme for music app specific colors
 * Provides easy access to theme-aware colors for music player components
 */
object RhythmColors {
    
    /**
     * Player-specific colors that adapt to the current theme
     */
    val playerBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background
    
    val playerButton: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
    
    val playerProgress: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary
    
    val playerProgressBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainer
    
    /**
     * Status colors that adapt to the current theme
     */
    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) SuccessDark else SuccessLight
    
    val warning: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) WarningDark else WarningLight
    
    /**
     * Surface variants for different elevation levels
     * Following Material Design 3 surface container tokens
     */
    val surfaceContainerLowest: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerLowest
    
    val surfaceContainerLow: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerLow
    
    val surfaceContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainer
    
    val surfaceContainerHigh: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh
    
    val surfaceContainerHighest: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest
}
