/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.navigation

/**
 * Main navigation screens for mode selection and top-level routing.
 */
sealed class MainScreen(val route: String) {
    /**
     * Mode selector screen - allows switching between local and streaming modes.
     */
    object ModeSelector : MainScreen("mode_selector")
    
    /**
     * Local music feature graph.
     */
    object Local : MainScreen("local") {
        fun createRoute(screen: String) = "local/$screen"
    }
    
    /**
     * Streaming music feature graph.
     */
    object Streaming : MainScreen("streaming") {
        fun createRoute(screen: String) = "streaming/$screen"
    }
    
    /**
     * Settings screen - shared between modes.
     */
    object Settings : MainScreen("settings")
    
    /**
     * Player screen - unified player for both modes.
     */
    object Player : MainScreen("player")
}
