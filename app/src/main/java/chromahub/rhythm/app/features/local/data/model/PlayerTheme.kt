/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.local.data.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R

/**
 * Defines the visual theme configuration for the player controls
 * including colors, shapes, sizes, and button styles
 */
data class PlayerTheme(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    
    // Control Button Sizes
    val playPauseButtonSize: ButtonSize,
    val prevNextButtonSize: ButtonSize,
    val seekButtonSize: ButtonSize,
    val toggleButtonSize: ButtonSize,
    
    // Shape and Corner Radius
    val controlGroupCornerRadius: Dp,
    val buttonCornerRadius: Dp,
    
    // Spacing
    val buttonSpacing: Dp,
    val groupSpacing: Dp,
    
    // Visual Style
    val controlGroupElevation: Dp,
    val buttonStyle: ButtonStyle,
    
    // Animation Settings
    val enableMorphingButtons: Boolean,
    val enableScaleAnimations: Boolean,
    val showPauseText: Boolean,
    val showToggleLabels: Boolean
) {
    data class ButtonSize(
        val extraSmall: Dp,
        val compact: Dp,
        val normal: Dp
    )
    
    enum class ButtonStyle {
        FILLED,           // FilledIconButton style
        TONAL,            // FilledTonalIconButton style
        OUTLINED,         // OutlinedIconButton style
        TEXT              // IconButton style
    }
    
    companion object {
        /**
         * Default theme - Current implementation
         * Expressive Material 3 design with morphing buttons
         */
        val DEFAULT = PlayerTheme(
            id = "default",
            nameRes = R.string.player_theme_default_name,
            descriptionRes = R.string.player_theme_default_desc,
            
            // Button Sizes
            playPauseButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 54.dp,
                normal = 60.dp
            ),
            prevNextButtonSize = ButtonSize(
                extraSmall = 42.dp,
                compact = 46.dp,
                normal = 50.dp
            ),
            seekButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 54.dp,
                normal = 60.dp
            ),
            toggleButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 48.dp,
                normal = 48.dp
            ),
            
            // Shapes
            controlGroupCornerRadius = 40.dp,
            buttonCornerRadius = 24.dp,
            
            // Spacing
            buttonSpacing = 8.dp,
            groupSpacing = 28.dp,
            
            // Visual Style
            controlGroupElevation = 2.dp,
            buttonStyle = ButtonStyle.FILLED,
            
            // Animations
            enableMorphingButtons = true,
            enableScaleAnimations = true,
            showPauseText = true,
            showToggleLabels = true
        )
        
        /**
         * Compact theme - Minimal spacing and smaller buttons
         */
        val COMPACT = PlayerTheme(
            id = "compact",
            nameRes = R.string.player_theme_compact_name,
            descriptionRes = R.string.player_theme_compact_desc,
            
            playPauseButtonSize = ButtonSize(
                extraSmall = 44.dp,
                compact = 48.dp,
                normal = 52.dp
            ),
            prevNextButtonSize = ButtonSize(
                extraSmall = 38.dp,
                compact = 42.dp,
                normal = 46.dp
            ),
            seekButtonSize = ButtonSize(
                extraSmall = 44.dp,
                compact = 48.dp,
                normal = 52.dp
            ),
            toggleButtonSize = ButtonSize(
                extraSmall = 44.dp,
                compact = 44.dp,
                normal = 44.dp
            ),
            
            controlGroupCornerRadius = 32.dp,
            buttonCornerRadius = 20.dp,
            
            buttonSpacing = 4.dp,
            groupSpacing = 16.dp,
            
            controlGroupElevation = 1.dp,
            buttonStyle = ButtonStyle.FILLED,
            
            enableMorphingButtons = false,
            enableScaleAnimations = true,
            showPauseText = false,
            showToggleLabels = false
        )
        
        /**
         * Large theme - Bigger buttons for better accessibility
         */
        val LARGE = PlayerTheme(
            id = "large",
            nameRes = R.string.player_theme_large_name,
            descriptionRes = R.string.player_theme_large_desc,
            
            playPauseButtonSize = ButtonSize(
                extraSmall = 56.dp,
                compact = 64.dp,
                normal = 72.dp
            ),
            prevNextButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 54.dp,
                normal = 60.dp
            ),
            seekButtonSize = ButtonSize(
                extraSmall = 56.dp,
                compact = 64.dp,
                normal = 72.dp
            ),
            toggleButtonSize = ButtonSize(
                extraSmall = 52.dp,
                compact = 52.dp,
                normal = 52.dp
            ),
            
            controlGroupCornerRadius = 44.dp,
            buttonCornerRadius = 28.dp,
            
            buttonSpacing = 12.dp,
            groupSpacing = 32.dp,
            
            controlGroupElevation = 2.dp,
            buttonStyle = ButtonStyle.FILLED,
            
            enableMorphingButtons = true,
            enableScaleAnimations = true,
            showPauseText = true,
            showToggleLabels = true
        )
        
        /**
         * Minimal theme - Clean and simple design
         */
        val MINIMAL = PlayerTheme(
            id = "minimal",
            nameRes = R.string.player_theme_minimal_name,
            descriptionRes = R.string.player_theme_minimal_desc,
            
            playPauseButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 52.dp,
                normal = 56.dp
            ),
            prevNextButtonSize = ButtonSize(
                extraSmall = 44.dp,
                compact = 48.dp,
                normal = 52.dp
            ),
            seekButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 52.dp,
                normal = 56.dp
            ),
            toggleButtonSize = ButtonSize(
                extraSmall = 48.dp,
                compact = 48.dp,
                normal = 48.dp
            ),
            
            controlGroupCornerRadius = 24.dp,
            buttonCornerRadius = 16.dp,
            
            buttonSpacing = 8.dp,
            groupSpacing = 24.dp,
            
            controlGroupElevation = 0.dp,
            buttonStyle = ButtonStyle.TEXT,
            
            enableMorphingButtons = false,
            enableScaleAnimations = false,
            showPauseText = false,
            showToggleLabels = false
        )
        
        /**
         * All available themes
         */
        fun getAllThemes(): List<PlayerTheme> = listOf(
            DEFAULT,
            COMPACT,
            LARGE,
            MINIMAL
        )
        
        /**
         * Get theme by ID
         */
        fun getThemeById(id: String): PlayerTheme? {
            return getAllThemes().find { it.id == id }
        }
    }
}

/**
 * Helper extensions for PlayerTheme
 */
fun PlayerTheme.ButtonSize.getSize(
    isExtraSmallWidth: Boolean,
    isCompactWidth: Boolean
): Dp = when {
    isExtraSmallWidth -> extraSmall
    isCompactWidth -> compact
    else -> normal
}
