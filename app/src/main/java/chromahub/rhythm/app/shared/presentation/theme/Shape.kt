/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 Expressive Shape System
// Uses more organic, rounded shapes for expressive design language
val Shapes = Shapes(
    // Extra small components (small chips, badges)
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small components (chips, small buttons)
    small = RoundedCornerShape(12.dp),
    
    // Medium components (buttons, cards, FABs)
    medium = RoundedCornerShape(16.dp),
    
    // Large components (sheets, dialogs, large cards)
    large = RoundedCornerShape(24.dp),
    
    // Extra large components (full-width modals, prominent surfaces)
    extraLarge = RoundedCornerShape(32.dp)
)

// Expressive shape variants for M3 Expressive design
object ExpressiveShapeTokens {
    // Fully rounded pill shapes
    val Full = CircleShape
    
    // Squircle-inspired shapes (larger radius for organic feel)
    val ExtraLarge = RoundedCornerShape(32.dp)
    val Large = RoundedCornerShape(28.dp)
    val Medium = RoundedCornerShape(20.dp)
    val Small = RoundedCornerShape(14.dp)
    val ExtraSmall = RoundedCornerShape(10.dp)
    
    // Asymmetric shapes for specific components
    val TopSheet = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 28.dp,
        bottomEnd = 28.dp
    )
    val BottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val StartSheet = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 28.dp
    )
    val EndSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 0.dp,
        bottomStart = 28.dp,
        bottomEnd = 0.dp
    )
}

// Custom shapes for music app specific components (Expressive)
object MusicShapes {
    // Player components - more organic shapes
    val PlayerCard = RoundedCornerShape(28.dp)
    val PlayerControls = CircleShape
    val MiniPlayer = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val MiniPlayerFloating = RoundedCornerShape(24.dp)
    
    // Album and media display
    val AlbumCover = RoundedCornerShape(16.dp)
    val AlbumCoverLarge = RoundedCornerShape(20.dp)
    val AlbumCoverSmall = RoundedCornerShape(12.dp)
    
    // Cards and containers
    val PlaylistCard = RoundedCornerShape(20.dp)
    val SongCard = RoundedCornerShape(16.dp)
    val ArtistCard = RoundedCornerShape(24.dp)
    val StatCard = RoundedCornerShape(20.dp)
    
    // Input components
    val SearchBar = CircleShape // Full pill shape for search
    val TextField = RoundedCornerShape(16.dp)
    
    // Navigation elements
    val NavBarIndicator = CircleShape
    val NavBarContainer = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Chips and badges
    val Chip = CircleShape
    val Badge = RoundedCornerShape(8.dp)
    val Tag = RoundedCornerShape(12.dp)
    
    // Buttons
    val ButtonPill = CircleShape
    val ButtonRounded = RoundedCornerShape(16.dp)
    val IconButton = CircleShape
    
    // Dialogs and sheets
    val Dialog = RoundedCornerShape(28.dp)
    val BottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Progress indicators
    val ProgressTrack = CircleShape
    val Slider = CircleShape
}
