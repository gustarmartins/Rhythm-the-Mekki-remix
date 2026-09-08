/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Central place for shared UI constants and composition locals.
 */
object UiConstants {
    /** Height of the global Mini-player card itself (not including spacing) */
    val MiniPlayerHeight = 72.dp // Reduced from 88dp for better proportions
    
    /** Height of the bottom navigation bar - now responsive to DPI */
    val NavBarHeight = 80.dp // Base height, will be scaled by density
    
    /** Standard spacing between UI elements */
    val MiniPlayerSpacing = 8.dp
}

/**
 * CompositionLocal that provides dynamic bottom padding based on visible UI elements.
 * This is calculated in RhythmNavigation based on:
 * - Whether MiniPlayer is visible
 * - Whether NavBar is visible  
 * - System navigation bar height
 * Defaults to zero when not provided.
 */
val LocalMiniPlayerPadding = compositionLocalOf { PaddingValues(bottom = 0.dp) }
